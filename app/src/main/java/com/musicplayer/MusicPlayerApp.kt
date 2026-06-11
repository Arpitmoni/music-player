package com.musicplayer

import android.app.Application

/**
 * MusicPlayerApp
 *
 * Application entry point — a good place for future global initialization
 * (crash reporters, dependency injection frameworks, etc.).
 *
 * Currently kept minimal — just ensures the Application class is declared
 * in the manifest so Android uses it.
 */
class MusicPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Future: init Timber, Hilt, etc. here
    }
}
