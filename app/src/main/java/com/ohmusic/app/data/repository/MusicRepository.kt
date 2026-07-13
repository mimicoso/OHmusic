package com.ohmusic.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.ohmusic.app.R
import com.ohmusic.app.data.dao.SongDao
import com.ohmusic.app.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val MIN_SONG_DURATION_MS = 2 * 60 * 1_000L

internal fun isEligibleSongDuration(durationMs: Long): Boolean =
    durationMs > MIN_SONG_DURATION_MS

@Singleton
class MusicRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val songDao: SongDao
) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()

    suspend fun toggleFavorite(songId: Long) = songDao.toggleFavorite(songId)

    suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    suspend fun getCount(): Int = songDao.getCount()

    fun getSongByIdFlow(id: Long): Flow<Song?> = songDao.getSongByIdFlow(id)

    /** 扫描设备本地音乐文件 */
    suspend fun scanLocalMusic(): Int = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = buildString {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            append(" AND ${MediaStore.Audio.Media.DURATION} > ?")
        }
        val selectionArgs = arrayOf(MIN_SONG_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                songs.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: context.getString(R.string.unknown_title),
                        artist = cursor.getString(artistCol) ?: context.getString(R.string.unknown_artist),
                        album = cursor.getString(albumCol) ?: context.getString(R.string.unknown_album),
                        duration = cursor.getLong(durationCol),
                        albumId = cursor.getLong(albumIdCol),
                        uri = contentUri.toString(),
                        dateAdded = cursor.getLong(dateAddedCol)
                    )
                )
            }
        }

        // 再次校验扫描结果，避免个别系统的 MediaStore 忽略查询中的时长条件。
        val validSongs = songs.filter { isEligibleSongDuration(it.duration) }

        // 保留旧收藏状态
        val favoriteIds = songDao.getFavoriteIds().toSet()
        val songsWithFav = validSongs.map { song ->
            if (song.id in favoriteIds) song.copy(isFavorite = true) else song
        }

        songDao.replaceAll(songsWithFav)
        songsWithFav.size
    }
}
