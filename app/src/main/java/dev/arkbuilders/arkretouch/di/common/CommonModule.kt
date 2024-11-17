package dev.arkbuilders.arkretouch.di.common

import dev.arkbuilders.arkretouch.data.repo.FilesRepository
import dev.arkbuilders.arkretouch.data.repo.OldStorageRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object CommonModule : InjectionModule {

    override fun create() = module {
        single {
            OldStorageRepository(androidContext())
        }
        singleOf(::FilesRepository)
    }
}