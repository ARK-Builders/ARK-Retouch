package dev.arkbuilders.arkretouch

import android.app.Application
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arkretouch.di.EditModule
import dev.arkbuilders.arkretouch.di.common.CommonModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        FoldersRepo.init(this)

        setupKoin()
    }

    private fun setupKoin() {
        startKoin {
            androidContext(applicationContext)
            androidLogger(Level.INFO)
            modules(
                EditModule.create(),
                CommonModule.create(),
            )
        }
    }
}