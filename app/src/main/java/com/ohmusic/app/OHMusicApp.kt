package com.ohmusic.app

import android.app.Application
import com.ohmusic.app.util.CoverResolver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OHMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoverResolver.init(this)
    }
}
