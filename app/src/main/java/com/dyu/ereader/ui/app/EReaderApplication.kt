package com.dyu.ereader.ui.app

import android.app.Application
import com.dyu.ereader.data.repository.update.AppUpdateWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppUpdateWorker.schedule(this)
    }
}
