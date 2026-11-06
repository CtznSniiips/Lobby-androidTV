@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.ctznsniiips.lobby.data.ServerAddress

/**
 * "Who's requesting?" for TVs that may request as more than one Seerr account. Whoever requested here most
 * recently is focused first, so repeat requests are one press. Back cancels without requesting.
 */
@Composable
fun RequesterDialog(
    picker: RequestPicker,
    address: ServerAddress,
    onChoose: (seerrUserId: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = picker.requesters.indexOfFirst { it.id == picker.lastRequesterId }.coerceAtLeast(0)
    val focus = remember(picker) { FocusRequester() }
    LaunchedEffect(picker) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LobbyColors.background)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Who's requesting?", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                picker.item.title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))

            picker.requesters.forEachIndexed { index, who ->
                Button(
                    onClick = { onChoose(who.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == initial) Modifier.focusRequester(focus) else Modifier),
                ) {
                    val avatar = address.imageUrl(who.avatarUrl, width = 80)
                    if (avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Text(who.name, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
