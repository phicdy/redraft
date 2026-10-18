package com.phicdy.redraft.shared.ui.di

import com.phicdy.redraft.shared.data.di.dataModule
import com.phicdy.redraft.shared.llm.di.llmModule
import com.phicdy.redraft.shared.ui.main.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule: Module = module {
    viewModelOf(::MainViewModel)
}

/** All Koin modules, passed to startKoin by each app entry point. */
val redraftModules: List<Module> = listOf(llmModule, dataModule, uiModule)
