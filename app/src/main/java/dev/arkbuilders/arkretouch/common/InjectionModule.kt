package com.etherean.app.common.di

import org.koin.core.module.Module

fun interface InjectionModule {
    fun create(): Module
}