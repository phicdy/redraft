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
import com.phicdy.redraft.shared.ui.history.HistoryScreen
import com.phicdy.redraft.shared.ui.main.MainScreen
import com.phicdy.redraft.shared.ui.report.ReportScreen
import com.phicdy.redraft.shared.ui.settings.SettingsScreen
import com.phicdy.redraft.shared.ui.theme.RedraftTheme

/** Root composable shared by the Android and desktop apps. Koin must be started before this. */
@Composable
fun RedraftApp() {
    // A few screens one level below the main screen, so a plain state switch is enough; no navigation library.
    var destination by rememberSaveable { mutableStateOf(Destination.Main) }
    val backToMain = { destination = Destination.Main }
    RedraftTheme {
        when (destination) {
            Destination.Main -> MainDestination(
                onOpenHistory = { destination = Destination.History },
                onOpenReport = { destination = Destination.Report },
                onOpenSettings = { destination = Destination.Settings },
            )

            // HistoryScreen handles system back itself, because back first closes an open entry.
            Destination.History -> HistoryScreen(onBack = backToMain)

            // ReportScreen also handles system back itself, because back first closes an open report.
            Destination.Report -> ReportScreen(onBack = backToMain)

            Destination.Settings -> {
                PlatformBackHandler(onBack = backToMain)
                SettingsScreen(onBack = backToMain)
            }
        }
    }
}

private enum class Destination {
    Main,
    History,
    Report,
    Settings,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainDestination(onOpenHistory: () -> Unit, onOpenReport: () -> Unit, onOpenSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redraft") },
                actions = {
                    TextButton(onClick = onOpenHistory) { Text("履歴") }
                    TextButton(onClick = onOpenReport) { Text("レポート") }
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
