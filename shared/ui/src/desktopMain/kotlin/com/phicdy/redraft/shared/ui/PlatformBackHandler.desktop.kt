package com.phicdy.redraft.shared.ui

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(onBack: () -> Unit) {
    // The settings screen has its own "戻る" button; desktop has no system back action.
}
