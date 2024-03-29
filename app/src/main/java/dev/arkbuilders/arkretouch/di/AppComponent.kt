package dev.arkbuilders.arkretouch.di

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dev.arkbuilders.arkretouch.presentation.edit.EditViewModelFactory
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    fun editVMFactory(): EditViewModelFactory.Factory
    fun app(): Application
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance context: Context
        ): AppComponent
    }
}
