@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Connect: enter the server address, then show the pairing code for the owner to approve in the plugin's
 * admin page. Same flow as the Tizen app, so the plugin sees nothing new.
 */
@Composable
fun ConnectScreen(state: ConnectState, initialAddress: String, onConnect: (String) -> Unit, onCancel: () -> Unit) {
    var address by remember { mutableStateOf(initialAddress.removePrefix("http://").removePrefix("https://")) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.pairingCode) {
        if (state.pairingCode == null) runCatching { focus.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(LobbyColors.background), contentAlignment = Alignment.Center) {
        Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Lobby", fontSize = 44.sp, fontWeight = FontWeight.Bold)

            if (state.pairingCode != null) {
                Text("Approve this code on ${state.serverName ?: "your server"}", fontSize = 20.sp)
                Text(state.pairingCode, fontSize = 72.sp, fontWeight = FontWeight.Bold, color = LobbyColors.accent)
                Text(
                    "Open the Lobby plugin page in the server dashboard, find this code under pending pairings and approve it. " +
                        "This screen continues on its own.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onCancel) { Text("Cancel") }
            } else {
                Text("Enter your Jellyfin or Emby server address", fontSize = 20.sp)
                BasicTextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 24.sp),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onConnect(address) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .border(2.dp, LobbyColors.accent, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                )
                Text(
                    "An IP address connects over http on port 8096. A host name connects over https.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.error != null) Text(state.error, color = LobbyColors.error, fontSize = 16.sp)
                Button(onClick = { onConnect(address) }, enabled = !state.busy) {
                    Text(if (state.busy) "Connecting…" else "Connect")
                }
            }
        }
    }
}
