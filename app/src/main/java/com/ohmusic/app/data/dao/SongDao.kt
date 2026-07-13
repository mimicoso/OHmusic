package com.ohmusic.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ohmusic.app.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongByIdFlow(id: Long): Flow<Song?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getCount(): Int

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("UPDATE songs SET isFavorite = NOT isFavorite WHERE id = :songId")
    suspend fun toggleFavorite(songId: Long)

    @Query("SELECT id FROM songs WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<Long>

    /**
     * 原子替换扫描结果。Room 会在事务提交后才通知 Flow，扫描或写入失败时
     * 旧数据会完整保留，不会向界面短暂发射空列表。
     */
    @Transaction
    suspend fun replaceAll(songs: List<Song>) {
        deleteAll()
        insertAll(songs)
    }
}
