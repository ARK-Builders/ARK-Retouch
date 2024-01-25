package dev.arkbuilders.arkretouch.di.common

import org.koin.core.module.Module

fun interface InjectionModule {
    fun create(): Module
}