package dev.arkbuilders.arkretouch.presentation

import android.app.Application
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arkretouch.di.DIManager
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DIManager.init(this)
        Timber.plant(Timber.DebugTree())
        FoldersRepo.init(this)
    }
}
