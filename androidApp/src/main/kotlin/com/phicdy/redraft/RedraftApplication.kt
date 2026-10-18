package com.phicdy.redraft

import android.app.Application
import com.phicdy.redraft.shared.ui.di.redraftModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RedraftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RedraftApplication)
            modules(redraftModules)
        }
    }
}
