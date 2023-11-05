package dev.arkbuilders.arkretouch.edit

import com.etherean.app.common.di.InjectionModule
import dev.arkbuilders.arkretouch.edit.ui.main.EditViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

object EditionModule : InjectionModule {

    override fun create() = module {
        viewModelOf(::EditViewModel)
    }
}