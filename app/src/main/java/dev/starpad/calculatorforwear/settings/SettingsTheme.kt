package dev.starpad.calculatorforwear.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * Wear Material 3 theme for settings.
 *
 * Follows the system watch face colors when the platform exposes them, so settings match the rest
 * of the watch instead of the calculator brand color.
 */
@Composable
fun SettingsTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val colorScheme = remember(context) { dynamicColorScheme(context) ?: ColorScheme() }
  MaterialTheme(colorScheme = colorScheme, content = content)
}
