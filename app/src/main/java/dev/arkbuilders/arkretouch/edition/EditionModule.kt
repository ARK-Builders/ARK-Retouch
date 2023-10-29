package dev.arkbuilders.arkretouch.edition

import com.etherean.app.common.di.InjectionModule
import dev.arkbuilders.arkretouch.edition.ui.main.EditViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

object EditionModule : InjectionModule {

    override fun create() = module {
        viewModelOf(::EditViewModel)
    }
}