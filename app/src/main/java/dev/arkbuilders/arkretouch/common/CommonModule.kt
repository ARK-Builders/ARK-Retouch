package dev.arkbuilders.arkretouch.common

import com.etherean.app.common.di.InjectionModule
import dev.arkbuilders.arkretouch.edition.repository.FilesRepository
import dev.arkbuilders.arkretouch.storage.OldStorageRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object CommonModule : InjectionModule {

    override fun create() = module {
        singleOf(::OldStorageRepository)
        singleOf(::FilesRepository)
    }
}