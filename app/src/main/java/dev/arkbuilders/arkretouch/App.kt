package dev.arkbuilders.arkretouch

import android.app.Application
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arkretouch.common.CommonModule
import dev.arkbuilders.arkretouch.edition.EditionModule
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
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

        setupAcra()
    }

    // FIXME: Let's use Firebase crashlytics instead
    private fun setupAcra() {
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

    private fun setupKoin() {
        startKoin {
            androidContext(applicationContext)
            androidLogger(Level.INFO)
            modules(
                EditionModule.create(),
                CommonModule.create(),
            )
        }
    }
}