package com.phicdy.redraft.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.phicdy.redraft.shared.ui.main.MainScreen

/** Root composable shared by the Android and desktop apps. Koin must be started before this. */
@Composable
fun RedraftApp() {
    MaterialTheme {
        MainScreen()
    }
}
