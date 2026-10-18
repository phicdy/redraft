package com.phicdy.redraft.shared.ui

import androidx.compose.runtime.Composable
import com.phicdy.redraft.shared.ui.main.MainScreen
import com.phicdy.redraft.shared.ui.theme.RedraftTheme

/** Root composable shared by the Android and desktop apps. Koin must be started before this. */
@Composable
fun RedraftApp() {
    RedraftTheme {
        MainScreen()
    }
}
