@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

private const val PACKAGE = "io.github.ctznsniiips.lobby"

/**
 * Makes the remote's Home button open Lobby. Android doesn't let an app do this silently and TVs differ, so the
 * options go from most to least convenient: the system's role prompt, the TV's own home-app setting, then the
 * one-line adb command that works on any build with network debugging.
 */
@Composable
fun HomeSetupScreen(
    isDefaultHome: Boolean,
    message: String?,
    onSetHome: () -> Unit,
    onHomeSettings: () -> Unit,
    onDone: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(isDefaultHome) { runCatching { focus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("Make Lobby your home screen", fontSize = 32.sp, fontWeight = FontWeight.Bold) }

            item {
                Text(
                    if (isDefaultHome) "Lobby is your home screen. The Home button on your remote opens it."
                    else "Right now the Home button opens a different launcher.",
                    fontSize = 18.sp,
                    color = if (isDefaultHome) LobbyColors.accent else MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!isDefaultHome) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onSetHome, modifier = Modifier.focusRequester(focus)) { Text("Set Lobby as the home app") }
                        Button(onClick = onHomeSettings) { Text("Open Android's home-app setting") }
                    }
                }
                item { Hint("Try the first button. If your TV shows no prompt, or the setting isn't there, use the command below.") }

                item { Text("If your TV won't switch", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp)) }
                item { Hint("From a computer, with network debugging turned on for the TV (Developer options):") }
                item { Command("adb connect <TV address>") }
                item { Command("adb shell cmd role add-role-holder android.app.role.HOME $PACKAGE") }
                item { Hint("On Android 9 or older:") }
                item { Command("adb shell pm set-home-activity $PACKAGE/.MainActivity") }
                item { Hint("To undo it:") }
                item { Command("adb shell cmd role remove-role-holder android.app.role.HOME $PACKAGE") }
                item { Hint("Some TVs keep their own home screen for the Home button whatever is chosen here.") }
            }

            item {
                Button(
                    onClick = onDone,
                    modifier = if (isDefaultHome) Modifier.focusRequester(focus) else Modifier,
                ) { Text("Done") }
            }
        }
        MessageToast(message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Command(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
