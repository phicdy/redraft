package com.phicdy.redraft.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Forces light (false) or dark (true) theme regardless of the system setting.
 * Used by preview screenshot tests to render both variants; null follows the system.
 */
internal val LocalDarkThemeOverride = staticCompositionLocalOf<Boolean?> { null }

@Composable
fun RedraftTheme(
    darkTheme: Boolean = LocalDarkThemeOverride.current ?: isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
