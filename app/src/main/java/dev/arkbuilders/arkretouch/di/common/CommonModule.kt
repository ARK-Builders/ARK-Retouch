package dev.arkbuilders.arkretouch.di.common

import dev.arkbuilders.arkretouch.editing.repository.FilesRepository
import dev.arkbuilders.arkretouch.storage.OldStorageRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object CommonModule : InjectionModule {

    override fun create() = module {
        singleOf(::OldStorageRepository)
        singleOf(::FilesRepository)
    }
}