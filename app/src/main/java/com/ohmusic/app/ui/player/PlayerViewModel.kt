package com.ohmusic.app.ui.player

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohmusic.app.data.model.Song
import com.ohmusic.app.data.repository.MusicRepository
import com.ohmusic.app.player.MusicPlayerManager
import com.ohmusic.app.player.PlayMode
import com.ohmusic.app.util.LyricLine
import com.ohmusic.app.util.LyricParser

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playMode: PlayMode = PlayMode.SEQUENCE,
    val isFavorite: Boolean = false,
    val lyrics: List<LyricLine> = emptyList()
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playerManager: MusicPlayerManager,
    private val repository: MusicRepository
) : ViewModel() {

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())

    private data class PlaybackState(
        val song: Song?,
        val playing: Boolean,
        val progress: Float,
        val position: Long,
        val duration: Long
    )

    private val playbackState = combine(
        playerManager.currentSong,
        playerManager.isPlaying,
        playerManager.progress,
        playerManager.currentPosition,
        playerManager.duration
    ) { song, playing, progress, position, duration ->
        PlaybackState(song, playing, progress, position, duration)
    }

    private val favoriteState = combine(
        playerManager.currentSong,
        repository.allSongs
    ) { currentSong, songs ->
        currentSong?.let { song -> songs.firstOrNull { it.id == song.id }?.isFavorite }
            ?: false
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackState,
        playerManager.playMode,
        favoriteState,
        _lyrics
    ) { playback, playMode, isFavorite, lyrics ->
        PlayerUiState(
            currentSong = playback.song,
            isPlaying = playback.playing,
            progress = playback.progress,
            currentPosition = playback.position,
            duration = playback.duration,
            playMode = playMode,
            isFavorite = isFavorite,
            lyrics = lyrics
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState()
    )

    fun loadLyrics(song: Song) {
        viewModelScope.launch {
            Log.d("PlayerViewModel", "Loading lyrics for song: ${song.title}, uri: ${song.uri}")
            val lyrics = LyricParser.parseFromFilePath(context, song.uri)
            Log.d("PlayerViewModel", "Loaded ${lyrics.size} lyric lines")
            _lyrics.value = lyrics
        }
    }

    fun connect() = playerManager.connect()
    fun togglePlay() = playerManager.togglePlay()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun seekTo(fraction: Float) = playerManager.seekTo(fraction)
    fun togglePlayMode() = playerManager.togglePlayMode()

    fun toggleFavorite() {
        val songId = uiState.value.currentSong?.id ?: return
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }
}
