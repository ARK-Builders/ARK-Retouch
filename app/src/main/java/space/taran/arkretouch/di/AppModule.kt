package space.taran.arkretouch.di

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {

    @Singleton
    @Provides
    fun screenDensity(app: Application): Float = app.resources.displayMetrics.density
}
