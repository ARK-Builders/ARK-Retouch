package space.taran.arkretouch.presentation

import android.app.Application
import space.taran.arkretouch.di.DIManager
import timber.log.Timber

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        DIManager.init(this)
        Timber.plant(Timber.DebugTree())
    }
}