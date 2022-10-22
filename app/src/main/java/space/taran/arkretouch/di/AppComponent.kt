package space.taran.arkretouch.di

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import space.taran.arkretouch.presentation.edit.EditViewModelFactory
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    fun editVMFactory(): EditViewModelFactory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance context: Context
        ): AppComponent
    }
}