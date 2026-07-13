package com.ohmusic.app.di

import android.content.Context
import androidx.room.Room
import com.ohmusic.app.data.dao.SongDao
import com.ohmusic.app.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ohmusic.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(db: AppDatabase): SongDao = db.songDao()
}
