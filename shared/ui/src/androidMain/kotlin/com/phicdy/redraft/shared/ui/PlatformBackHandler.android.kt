package com.phicdy.redraft.shared.ui

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
internal actual fun PlatformBackHandler(onBack: () -> Unit) {
    val state = rememberNavigationEventState<NavigationEventInfo>(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = state,
        isBackEnabled = true,
        onBackCompleted = onBack,
    )
}
