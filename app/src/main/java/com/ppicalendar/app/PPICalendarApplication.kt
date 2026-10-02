package com.ppicalendar.app

import android.app.Application
import com.ppicalendar.app.di.AppContainer
import com.ppicalendar.app.di.DefaultAppContainer

class PPICalendarApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Ensure default 'Syncly' folder exists in internal/external storage
        try {
            java.io.File(filesDir, "Syncly").mkdirs()
            getExternalFilesDir(null)?.let { ext ->
                java.io.File(ext, "Syncly").mkdirs()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
