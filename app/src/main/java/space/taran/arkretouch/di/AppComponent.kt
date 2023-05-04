package space.taran.arkretouch.di

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import space.taran.arkretouch.Preferences
import space.taran.arkretouch.presentation.edit.EditViewModelFactory
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    fun editVMFactory(): EditViewModelFactory.Factory
    fun app(): Application
    fun prefs(): Preferences

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance context: Context
        ): AppComponent
    }
}
