package com.phicdy.redraft.shared.ui.screenshot

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.phicdy.redraft.shared.ui.theme.LocalDarkThemeOverride
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders every entry of [previewCatalog] in light and dark theme to
 * `<name>_Light.png` / `<name>_Dark.png`. CI publishes them as a PR comment.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewScreenshotTest {
    @Test
    fun renderPreviews() {
        val outputDir = File(System.getProperty("redraft.screenshotDir") ?: "build/screenshots")
        outputDir.mkdirs()

        for (entry in previewCatalog) {
            for (variant in Variant.entries) {
                val file = File(outputDir, "${entry.name}_${variant.name}.png")
                runDesktopComposeUiTest(width = WIDTH_PX, height = HEIGHT_PX) {
                    setContent {
                        CompositionLocalProvider(
                            LocalDensity provides Density(DENSITY),
                            LocalDarkThemeOverride provides variant.isDark,
                        ) {
                            entry.content()
                        }
                    }
                    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
                }
                assertTrue(file.length() > 0, "Failed to write ${file.name}")
            }
        }
    }

    private enum class Variant(val isDark: Boolean) {
        Light(isDark = false),
        Dark(isDark = true),
    }

    private companion object {
        // 400 x 800 dp, a typical phone size, rendered at 2x for sharp images.
        const val DENSITY = 2f
        const val WIDTH_PX = 800
        const val HEIGHT_PX = 1600
    }
}
