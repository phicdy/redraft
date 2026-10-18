package com.phicdy.redraft.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.phicdy.redraft.shared.ui.RedraftApp
import com.phicdy.redraft.shared.ui.di.redraftModules
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(redraftModules)
    }
    application {
        Window(onCloseRequest = ::exitApplication, title = "Redraft") {
            RedraftApp()
        }
    }
}
