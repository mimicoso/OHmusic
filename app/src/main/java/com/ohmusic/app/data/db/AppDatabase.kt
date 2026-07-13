package com.ohmusic.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ohmusic.app.data.dao.SongDao
import com.ohmusic.app.data.model.Song

@Database(
    entities = [Song::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        /** 版本 1 → 2：添加 isFavorite 字段 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
