package dev.arkbuilders.arkretouch.presentation

import android.app.Application
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import dev.arkbuilders.arkretouch.BuildConfig
import dev.arkbuilders.arkretouch.R
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arkretouch.di.DIManager
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DIManager.init(this)
        Timber.plant(Timber.DebugTree())
        FoldersRepo.init(this)

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            dialog {
                text = getString(R.string.crash_dialog_description)
                title = getString(R.string.crash_dialog_title)
                commentPrompt = getString(R.string.crash_dialog_comment)
            }
            httpSender {
                uri = BuildConfig.ACRA_URI
                basicAuthLogin = BuildConfig.ACRA_LOGIN
                basicAuthPassword = BuildConfig.ACRA_PASS
                httpMethod = HttpSender.Method.POST
            }
        }
    }
}
