package dev.starpad.calculatorforwear.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.starpad.calculatorforwear.R

/**
 * Watch-native settings list.
 *
 * Stateless so the hosting activity keeps ownership of persistence: every action is reported to the
 * caller, including history clearing, which is only reported once the user confirms.
 */
@Composable
fun SettingsScreen(
  hapticsEnabled: Boolean,
  onHapticsEnabledChange: (Boolean) -> Unit,
  onShowTutorial: () -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var confirmingClearHistory by rememberSaveable { mutableStateOf(false) }
  val columnState = rememberTransformingLazyColumnState()
  val transformationSpec = rememberTransformationSpec()

  AppScaffold(modifier = modifier) {
    ScreenScaffold(scrollState = columnState) { contentPadding ->
      TransformingLazyColumn(
        state = columnState,
        contentPadding = contentPadding,
        // Rotary feedback goes straight to the platform instead of through the Compose haptics
        // local, so the preference has to be handed to it explicitly.
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(
          scrollableState = columnState,
          hapticFeedbackEnabled = hapticsEnabled,
        ),
      ) {
        item {
          ListHeader(
            modifier = Modifier.listItem(this@item, transformationSpec),
            transformation = SurfaceTransformation(transformationSpec),
          ) {
            Text(stringResource(R.string.settings_title))
          }
        }
        item {
          SwitchButton(
            checked = hapticsEnabled,
            onCheckedChange = onHapticsEnabledChange,
            modifier = Modifier.listItem(this@item, transformationSpec),
            transformation = SurfaceTransformation(transformationSpec),
            label = { Text(stringResource(R.string.haptics_enabled)) },
          )
        }
        item {
          Button(
            onClick = onShowTutorial,
            modifier = Modifier.listItem(this@item, transformationSpec),
            transformation = SurfaceTransformation(transformationSpec),
            colors = ButtonDefaults.filledTonalButtonColors(),
            label = { Text(stringResource(R.string.show_tutorial)) },
          )
        }
        item {
          Button(
            onClick = { confirmingClearHistory = true },
            modifier = Modifier.listItem(this@item, transformationSpec),
            transformation = SurfaceTransformation(transformationSpec),
            colors = destructiveButtonColors(),
            label = { Text(stringResource(R.string.clear_history)) },
          )
        }
      }
    }
  }

  ClearHistoryDialog(
    visible = confirmingClearHistory,
    onConfirm = {
      confirmingClearHistory = false
      onClearHistory()
    },
    onDismiss = { confirmingClearHistory = false },
  )
}

/** Full-width item sized so the list can scale and morph it while scrolling. */
private fun Modifier.listItem(scope: TransformingLazyColumnItemScope, spec: TransformationSpec) =
  fillMaxWidth().transformedHeight(scope, spec)

/** Confirmation for the destructive history reset, dismissible by cancel, back, or swipe. */
@Composable
private fun ClearHistoryDialog(
  visible: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    visible = visible,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.clear_history_title)) },
    text = { Text(stringResource(R.string.clear_history_message)) },
    transformationSpec = rememberTransformationSpec(),
  ) {
    item {
      Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        colors = destructiveButtonColors(),
        label = { DialogActionLabel(stringResource(R.string.clear_history_confirm)) },
      )
    }
    item {
      Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(),
        label = { DialogActionLabel(stringResource(android.R.string.cancel)) },
      )
    }
  }
}

/** Dialog actions carry no icon, so their labels are centered instead of list-aligned. */
@Composable
private fun DialogActionLabel(text: String) {
  Text(text = text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

/** Error colors marking the history reset as destructive. */
@Composable
private fun destructiveButtonColors() = ButtonDefaults.buttonColors(
  containerColor = MaterialTheme.colorScheme.errorContainer,
  contentColor = MaterialTheme.colorScheme.onErrorContainer,
)
