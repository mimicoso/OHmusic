package com.ohmusic.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@UnstableApi
@RunWith(AndroidJUnit4::class)
class FallbackBitmapLoaderTest {

    @Test
    fun missingArtworkReturnsDefaultBitmap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = FallbackBitmapLoader(context)
            .loadBitmap(Uri.parse("content://com.ohmusic.missing/cover"))
            .get(5, TimeUnit.SECONDS)

        assertTrue(bitmap.width > 0)
        assertTrue(bitmap.height > 0)
    }
}
