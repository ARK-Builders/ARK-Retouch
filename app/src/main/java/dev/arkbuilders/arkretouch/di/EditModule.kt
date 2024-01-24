package dev.arkbuilders.arkretouch.di

import com.etherean.app.common.di.InjectionModule
import dev.arkbuilders.arkretouch.editing.ui.main.EditViewModel
import dev.arkbuilders.arkretouch.storage.Resolution
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.nio.file.Path

object EditModule : InjectionModule {

    override fun create() = module {
        viewModel {
                (
                    primaryColor: Long,
                    launchedFromIntent: Boolean,
                    imagePath: Path?,
                    imageUri: String?,
                    maxResolution: Resolution
                ) ->
            EditViewModel(primaryColor, launchedFromIntent, imagePath, imageUri, maxResolution, get())
        }
    }
}