package com.ohmusic.app.ui.playlist

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ohmusic.app.R
import androidx.lifecycle.viewModelScope
import com.ohmusic.app.data.model.Song
import com.ohmusic.app.data.repository.MusicRepository
import com.ohmusic.app.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val songs: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isScanning: Boolean = false,
    val errorMessage: String? = null
)

private data class PlaylistOperationState(
    val isScanning: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val operation = MutableStateFlow(PlaylistOperationState())
    private var initialScanAttempted = false

    val uiState: StateFlow<PlaylistUiState> = combine(
        repository.allSongs,
        repository.favoriteSongs,
        playerManager.currentSong,
        operation
    ) { songs, favoriteSongs, currentSong, operation ->
        PlaylistUiState(
            songs = songs,
            favoriteSongs = favoriteSongs,
            currentSong = currentSong,
            isScanning = operation.isScanning,
            errorMessage = operation.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaylistUiState()
    )

    fun ensureInitialScan(hasPermission: Boolean) {
        if (!hasPermission || initialScanAttempted) return
        initialScanAttempted = true
        // 每次进程首次进入歌单都与 MediaStore 同步，及时反映新增和已删除歌曲。
        viewModelScope.launch { scan() }
    }

    fun refresh(hasPermission: Boolean, onPermissionRequired: () -> Unit) {
        if (!hasPermission) {
            onPermissionRequired()
            return
        }
        viewModelScope.launch { scan() }
    }

    fun play(song: Song, songs: List<Song>) = playerManager.play(song, songs)

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    private suspend fun scan() {
        if (operation.value.isScanning) return
        try {
            operation.value = PlaylistOperationState(isScanning = true)
            repository.scanLocalMusic()
            operation.value = PlaylistOperationState()
        } catch (error: Exception) {
            operation.value = PlaylistOperationState(
                errorMessage = error.message ?: context.getString(R.string.scan_failed)
            )
        }
    }
}
