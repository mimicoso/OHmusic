package com.ohmusic.app.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ohmusic.app.data.model.Song
import com.ohmusic.app.data.repository.MusicRepository
import com.ohmusic.app.util.CoverResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI 面向的 MediaController 门面。
 *
 * 播放器本身位于 [MusicPlayerService]，因此 Activity 销毁后播放不会中断。
 */
@Singleton
class MusicPlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressJob: Job? = null
    private var playlist: List<Song> = emptyList()
    private var songsById: Map<String, Song> = emptyMap()
    private var lastPersistedPosition = 0L

    private val prefs = context.getSharedPreferences("ohmusic_prefs", Context.MODE_PRIVATE)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playMode = MutableStateFlow(
        PlayMode.entries.getOrElse(prefs.getInt("play_mode", 0)) { PlayMode.SEQUENCE }
    )
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val controllerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaController = controller ?: return
            if (!mediaController.isConnected || mediaItem == null) return
            val position = if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                mediaController.currentPosition.coerceAtLeast(0L)
            } else {
                0L
            }
            persistCurrentMedia(mediaItem.mediaId, position)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val mediaController = controller ?: return
            if (mediaController.isConnected && !isPlaying && mediaController.mediaItemCount > 0) {
                persistPosition(mediaController.currentPosition.coerceAtLeast(0L))
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            syncState(player)
        }
    }

    /** 首帧后或首次播放操作时再连接 Service，避免播放器初始化阻塞冷启动关键路径。 */
    fun connect() {
        ensureController()
    }

    private fun ensureController() {
        if (controller?.isConnected == true || controllerFuture != null) return

        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
        ).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val connectedController = future.get()
                    controller = connectedController
                    connectedController.addListener(controllerListener)
                    applyPlayMode(connectedController)
                    syncState(connectedController)
                    pendingActions.toList().also { pendingActions.clear() }
                        .forEach { it(connectedController) }
                    scope.launch { restorePlaylistIfNeeded(connectedController) }
                } catch (error: Exception) {
                    controllerFuture = null
                    Log.e(TAG, "连接播放服务失败", error)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playlist = songs
        songsById = songs.associateBy { it.id.toString() }
        val safeIndex = startIndex.coerceIn(songs.indices)
        persistQueue(songs, safeIndex, 0L)
        withController { mediaController ->
            mediaController.setMediaItems(
                songs.map(::toMediaItem),
                safeIndex,
                0L
            )
            applyPlayMode(mediaController)
            mediaController.prepare()
            mediaController.play()
        }
    }

    fun play(song: Song, songs: List<Song> = playlist) {
        if (songs.isEmpty()) return
        val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        setPlaylist(songs, index)
    }

    fun togglePlay() = withController { player ->
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() = withController { player ->
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.mediaItemCount > 0 && _playMode.value == PlayMode.SEQUENCE) {
            player.seekToDefaultPosition(0)
        }
    }

    fun previous() = withController { player ->
        if (player.hasPreviousMediaItem()) {
            if (player.isPlaying && player.currentPosition > PREVIOUS_RESTART_THRESHOLD_MS) {
                player.seekTo(0)
            } else {
                player.seekToPreviousMediaItem()
            }
        } else if (player.mediaItemCount > 0 && _playMode.value == PlayMode.SEQUENCE) {
            player.seekToDefaultPosition(player.mediaItemCount - 1)
        }
    }

    fun seekTo(fraction: Float) = withController { player ->
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return@withController
        player.seekTo((fraction.coerceIn(0f, 1f) * duration).toLong())
        syncProgress(player)
    }

    fun togglePlayMode() {
        _playMode.value = nextPlayMode(_playMode.value)
        prefs.edit { putInt("play_mode", _playMode.value.ordinal) }
        withController(::applyPlayMode)
    }

    fun release() {
        progressJob?.cancel()
        controller?.removeListener(controllerListener)
        controller?.release()
        controller = null
        controllerFuture?.cancel(false)
        controllerFuture = null
    }

    private fun withController(action: (MediaController) -> Unit) {
        val current = controller
        if (current != null && current.isConnected) {
            action(current)
        } else {
            pendingActions += action
            ensureController()
        }
    }

    private fun applyPlayMode(player: Player) {
        player.repeatMode = repeatModeFor(_playMode.value)
        player.shuffleModeEnabled = shuffleEnabledFor(_playMode.value)
    }

    private fun syncState(player: Player) {
        if (player is MediaController && !player.isConnected) return
        _isPlaying.value = player.isPlaying
        val mediaId = player.currentMediaItem?.mediaId
        _currentSong.value = mediaId?.let(songsById::get) ?: _currentSong.value
        if (!mediaId.isNullOrEmpty()) {
            prefs.edit { putString(KEY_CURRENT_MEDIA_ID, mediaId) }
        }
        syncProgress(player)

        if (!player.isPlaying && player.mediaItemCount > 0) {
            persistPosition(player.currentPosition.coerceAtLeast(0L))
        }

        if (player.isPlaying) startProgressLoop() else stopProgressLoop()
    }

    private fun syncProgress(player: Player) {
        val safeDuration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        val safePosition = player.currentPosition.coerceAtLeast(0L)
        _duration.value = safeDuration
        _currentPosition.value = safePosition
        _progress.value = if (safeDuration > 0) {
            (safePosition.toFloat() / safeDuration).coerceIn(0f, 1f)
        } else {
            0f
        }
        if (kotlin.math.abs(safePosition - lastPersistedPosition) >= POSITION_SAVE_INTERVAL_MS) {
            persistPosition(safePosition)
        }
    }

    private fun startProgressLoop() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (true) {
                controller?.let(::syncProgress)
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun toMediaItem(song: Song): MediaItem = MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(song.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setArtworkUri(CoverResolver.resolve(song.albumId))
                .build()
        )
        .build()

    private suspend fun restorePlaylistIfNeeded(mediaController: MediaController) {
        if (!mediaController.isConnected || mediaController.mediaItemCount > 0) return
        val ids = prefs.getString(KEY_QUEUE_IDS, null)
            ?.split(',')
            ?.mapNotNull(String::toLongOrNull)
            .orEmpty()
        if (ids.isEmpty()) return

        val songsByStoredId = repository.allSongs.first().associateBy(Song::id)
        val restoredSongs = ids.mapNotNull(songsByStoredId::get)
        if (restoredSongs.isEmpty() || mediaController.mediaItemCount > 0) return

        playlist = restoredSongs
        songsById = restoredSongs.associateBy { it.id.toString() }
        val currentId = prefs.getString(KEY_CURRENT_MEDIA_ID, null)
        val currentIndex = restoredSongs.indexOfFirst { it.id.toString() == currentId }
            .coerceAtLeast(0)
        val position = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L)
        mediaController.setMediaItems(restoredSongs.map(::toMediaItem), currentIndex, position)
        applyPlayMode(mediaController)
        mediaController.prepare()
        syncState(mediaController)
    }

    private fun persistQueue(songs: List<Song>, currentIndex: Int, position: Long) {
        prefs.edit {
            putString(KEY_QUEUE_IDS, songs.joinToString(",") { it.id.toString() })
            putString(KEY_CURRENT_MEDIA_ID, songs.getOrNull(currentIndex)?.id?.toString())
            putLong(KEY_POSITION, position)
        }
        lastPersistedPosition = position
    }

    private fun persistPosition(position: Long) {
        lastPersistedPosition = position
        prefs.edit { putLong(KEY_POSITION, position) }
    }

    private fun persistCurrentMedia(mediaId: String, position: Long) {
        lastPersistedPosition = position
        prefs.edit {
            putString(KEY_CURRENT_MEDIA_ID, mediaId)
            putLong(KEY_POSITION, position)
        }
    }

    private companion object {
        const val TAG = "MusicPlayerManager"
        const val PROGRESS_INTERVAL_MS = 500L
        const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
        const val KEY_QUEUE_IDS = "queue_ids"
        const val KEY_CURRENT_MEDIA_ID = "current_media_id"
        const val KEY_POSITION = "position_ms"
    }
}

enum class PlayMode {
    SEQUENCE,
    REPEAT_ONE,
    SHUFFLE
}
