package com.ohmusic.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ohmusic.app.data.db.AppDatabase
import com.ohmusic.app.data.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun replaceAllReplacesTheOldSnapshot() = runBlocking {
        val dao = database.songDao()
        dao.replaceAll(listOf(song(1), song(2, favorite = true)))
        assertEquals(listOf(1L, 2L), dao.getAllSongs().first().map { it.id }.sorted())

        dao.replaceAll(listOf(song(3)))
        assertEquals(listOf(3L), dao.getAllSongs().first().map { it.id })
    }

    @Test
    fun favoriteQueriesAndToggleStayConsistent() = runBlocking {
        val dao = database.songDao()
        dao.replaceAll(listOf(song(1), song(2)))

        dao.toggleFavorite(2)

        assertEquals(listOf(2L), dao.getFavoriteIds())
        assertEquals(listOf(2L), dao.getFavoriteSongs().first().map { it.id })
    }

    private fun song(id: Long, favorite: Boolean = false) = Song(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        duration = 60_000,
        albumId = id,
        uri = "content://media/external/audio/media/$id",
        dateAdded = id,
        isFavorite = favorite
    )
}
