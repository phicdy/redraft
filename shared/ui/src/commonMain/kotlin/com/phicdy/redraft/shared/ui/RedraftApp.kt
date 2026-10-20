package com.phicdy.redraft.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.phicdy.redraft.shared.ui.main.MainScreen
import com.phicdy.redraft.shared.ui.settings.SettingsScreen
import com.phicdy.redraft.shared.ui.theme.RedraftTheme

/** Root composable shared by the Android and desktop apps. Koin must be started before this. */
@Composable
fun RedraftApp() {
    // Two screens only, so a plain state switch is enough; no navigation library.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    RedraftTheme {
        if (showSettings) {
            PlatformBackHandler(onBack = { showSettings = false })
            SettingsScreen(onBack = { showSettings = false })
        } else {
            MainDestination(onOpenSettings = { showSettings = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainDestination(onOpenSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redraft") },
                actions = {
                    TextButton(onClick = onOpenSettings) { Text("設定") }
                },
            )
        },
    ) { innerPadding ->
        // MainScreen has its own Scaffold; consuming the insets here keeps it from padding the status bar twice.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            MainScreen()
        }
    }
}
