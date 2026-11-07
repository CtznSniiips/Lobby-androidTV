@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.ctznsniiips.lobby.data.HomeItem
import io.github.ctznsniiips.lobby.data.HomeRow
import io.github.ctznsniiips.lobby.data.ServerAddress
import io.github.ctznsniiips.lobby.device.DockApp
import kotlinx.coroutines.delay

private const val LANDSCAPE_W = 280
private const val POSTER_W = 160

@Composable
fun HomeScreen(
    state: LobbyState,
    address: ServerAddress,
    onItem: (HomeItem) -> Unit,
    onApp: (DockApp) -> Unit,
    onSearch: () -> Unit,
    onScreenSaver: () -> Unit,
    onRefresh: () -> Unit,
    onLobbySettings: () -> Unit,
    onSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val home = state.home

    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(home?.tvName ?: "Lobby", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (state.loading) Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 24.dp))
                    Button(onClick = onScreenSaver, modifier = Modifier.padding(end = 12.dp)) { Text("Screen saver") }
                    Button(onClick = onSearch) { Text("Search") }
                }
            }

            state.homeError?.let { error ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error, color = LobbyColors.error)
                        Button(onClick = onRefresh) { Text("Try again") }
                    }
                }
            }

            if (home != null && home.featured.isNotEmpty()) {
                item { FeaturedRow(home.featured, address, onItem) }
            }

            home?.rows?.filter { it.items.isNotEmpty() }?.let { rows ->
                items(rows.distinctBy { it.id }, key = { it.id }) { row -> MediaRow(row, address, onItem) }
            }

            if (state.apps.isNotEmpty()) {
                item { AppDock(state.apps, onApp) }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onRefresh) { Text("Refresh") }
                    Button(onClick = onLobbySettings) { Text("Lobby settings") }
                    Button(onClick = onSettings) { Text("System settings") }
                    Button(onClick = onDisconnect) { Text("Disconnect") }
                }
            }
        }

        MessageToast(state.message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}

/** One-line feedback from an action. Clears itself after a few seconds. */
@Composable
fun MessageToast(message: String?, onShown: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(5000)
            onShown()
        }
    }
    if (message != null) {
        Box(
            modifier
                .padding(32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(LobbyColors.scrim)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) { Text(message, fontSize = 18.sp) }
    }
}

@Composable
private fun FeaturedRow(items: List<HomeItem>, address: ServerAddress, onItem: (HomeItem) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        items(items.distinctBy { it.id }, key = { it.id }) { item ->
            val art = address.imageUrl(item.backdropUrl ?: item.landscapeUrl, width = 1280)
            Card(onClick = { onItem(item) }, modifier = Modifier.width(560.dp)) {
                Box(Modifier.fillMaxWidth().height(315.dp)) {
                    if (art != null) {
                        AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(LobbyColors.scrim)
                            .padding(16.dp),
                    ) {
                        Text(item.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val line = item.tagline ?: item.meta
                        if (!line.isNullOrBlank()) {
                            Text(line, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MediaRow(row: HomeRow, address: ServerAddress, onItem: (HomeItem) -> Unit) {
    val poster = row.layout == "poster"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(row.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(row.items.distinctBy { it.id }, key = { it.id }) { item -> MediaCard(item, address, poster, onItem) }
        }
    }
}

@Composable
private fun MediaCard(item: HomeItem, address: ServerAddress, poster: Boolean, onItem: (HomeItem) -> Unit) {
    val width = if (poster) POSTER_W else LANDSCAPE_W
    val height = if (poster) 240 else 158
    val art = address.imageUrl(if (poster) item.posterUrl ?: item.landscapeUrl else item.landscapeUrl ?: item.posterUrl, width = width * 2)

    Card(onClick = { onItem(item) }, modifier = Modifier.width(width.dp)) {
        Box(Modifier.fillMaxWidth().height(height.dp)) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            item.progressPercent?.takeIf { it > 0 }?.let { percent ->
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth((percent / 100.0).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(LobbyColors.accent))
                }
            }
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(item.title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = item.subtitle ?: item.requestStatus?.takeIf { it != "none" }
            if (!sub.isNullOrBlank()) {
                Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AppDock(apps: List<DockApp>, onApp: (DockApp) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Apps", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps.distinctBy { it.packageName }, key = { it.packageName }) { app ->
                val bitmap = remember(app.packageName) { app.icon?.toBitmap(320, 180)?.asImageBitmap() }
                Card(onClick = { onApp(app) }, modifier = Modifier.width(200.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(112.dp),
                        )
                    }
                    Text(app.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
