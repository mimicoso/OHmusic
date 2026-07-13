package com.ohmusic.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,          // MediaStore ID
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,                 // 毫秒
    val albumId: Long,                  // 用于获取封面
    val uri: String,                    // content:// URI
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false     // 收藏标记
)
