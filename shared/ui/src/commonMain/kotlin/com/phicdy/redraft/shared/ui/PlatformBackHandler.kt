package com.phicdy.redraft.shared.ui

import androidx.compose.runtime.Composable

/** Calls [onBack] on the system back action (Android). Desktop has no system back, so it does nothing there. */
@Composable
internal expect fun PlatformBackHandler(onBack: () -> Unit)
