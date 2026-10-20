package com.phicdy.redraft.shared.ui.main.result

import androidx.compose.ui.platform.ClipEntry

/**
 * A clipboard entry holding [text], for [androidx.compose.ui.platform.Clipboard.setClipEntry].
 * Compose Multiplatform has no common factory for plain text yet, so each platform builds one.
 */
internal expect fun plainTextClipEntry(text: String): ClipEntry
