@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import io.github.ctznsniiips.lobby.data.HomeItem
import io.github.ctznsniiips.lobby.data.HomeRow
import io.github.ctznsniiips.lobby.data.ServerAddress
import io.github.ctznsniiips.lobby.device.DockApp
import io.github.ctznsniiips.lobby.device.TvInput
import kotlinx.coroutines.delay

private const val LANDSCAPE_W = 200
private const val LANDSCAPE_H = 112
private const val POSTER_W = 84
private const val POSTER_H = 126
private const val ROTATE_MS = 8000L

/**
 * Home, laid out like the Samsung app: the focused (or featured) title's backdrop fills the screen, a slim rail of
 * utilities runs down the left, the hero text sits top-left with its action button, and the rows and app dock
 * scroll underneath. Focusing a card makes the hero describe that title.
 */
@Composable
fun HomeScreen(
    state: LobbyState,
    address: ServerAddress,
    onItem: (HomeItem) -> Unit,
    onApp: (DockApp) -> Unit,
    onInput: (TvInput) -> Unit,
    onSearch: () -> Unit,
    onScreenSaver: () -> Unit,
    showHomePrompt: Boolean,
    onHomeSetup: () -> Unit,
    onHomePromptDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onLobbySettings: () -> Unit,
    onSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val home = state.home
    val featured = remember(home) { home?.featured.orEmpty().distinctBy { it.id } }
    var featuredIndex by remember { mutableIntStateOf(0) }
    var browsed by remember { mutableStateOf<HomeItem?>(null) }
    val heroItem = browsed ?: featured.getOrNull(featuredIndex.coerceIn(0, (featured.size - 1).coerceAtLeast(0)))

    val searchFocus = remember { FocusRequester() }
    val heroFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Rotate the featured titles while nothing in the rows is focused.
    LaunchedEffect(featured, browsed) {
        if (browsed == null && featured.size > 1) {
            while (true) {
                delay(ROTATE_MS)
                featuredIndex = (featuredIndex + 1) % featured.size
            }
        }
    }
    LaunchedEffect(featured.isNotEmpty()) {
        if (featured.isNotEmpty()) runCatching { heroFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        HeroBackdrop(heroItem, address)

        Row(Modifier.fillMaxSize()) {
            Rail(
                onSearch = onSearch,
                onScreenSaver = onScreenSaver,
                onApps = { /* the dock is the last list entry */ },
                onSettings = onLobbySettings,
                searchFocus = searchFocus,
                listState = listState,
            )

            Column(Modifier.fillMaxSize().padding(end = 40.dp, top = 24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(greeting(), fontSize = 16.sp, color = Color(0xCCFFFFFF), modifier = Modifier.weight(1f))
                    if (state.loading) Text("Loading…", fontSize = 14.sp, color = Color(0x99FFFFFF), modifier = Modifier.padding(end = 20.dp))
                    ClockText()
                }

                Hero(heroItem, featured.size, featuredIndex, onItem, searchFocus, heroFocus, Modifier.fillMaxWidth().height(190.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (showHomePrompt) {
                        item { HomePrompt(onHomeSetup, onHomePromptDismiss) }
                    }

                    state.homeError?.let { error ->
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(error, color = LobbyColors.error)
                                Button(onClick = onRefresh) { Text("Try again") }
                            }
                        }
                    }

                    home?.rows?.filter { it.items.isNotEmpty() }?.let { rows ->
                        items(rows.distinctBy { it.id }, key = { it.id }) { row ->
                            MediaRow(row, address, onItem, onFocused = { browsed = it })
                        }
                    }

                    if (state.inputs.isNotEmpty()) {
                        item { Box(Modifier.onFocusChanged { if (it.hasFocus) browsed = null }) { InputRow(state.inputs, onInput) } }
                    }

                    if (state.apps.isNotEmpty()) {
                        item { Box(Modifier.onFocusChanged { if (it.hasFocus) browsed = null }) { AppDock(state.apps, onApp) } }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onRefresh) { Text("Refresh") }
                            Button(onClick = onSettings) { Text("System settings") }
                            Button(onClick = onDisconnect) { Text("Disconnect") }
                        }
                    }
                }
            }
        }

        MessageToast(state.message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HeroBackdrop(item: HomeItem?, address: ServerAddress) {
    val art = address.imageUrl(item?.backdropUrl ?: item?.landscapeUrl, width = 1920)
    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = art, animationSpec = tween(600), label = "backdrop") { url ->
            if (url != null) {
                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        // Dark on the left so the text reads, and fading into the page colour where the rows sit.
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xE60E1116), Color(0x990E1116), Color.Transparent)),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.35f to Color.Transparent, 0.7f to Color(0xE60E1116), 1f to LobbyColors.background),
            ),
        )
    }
}

@Composable
private fun Hero(
    item: HomeItem?,
    count: Int,
    index: Int,
    onItem: (HomeItem) -> Unit,
    searchFocus: FocusRequester,
    heroFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        if (item != null) {
            Column(Modifier.align(Alignment.CenterStart).width(560.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, fontSize = 40.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val line = item.tagline ?: item.meta ?: item.subtitle
                if (!line.isNullOrBlank()) {
                    Text(line, fontSize = 16.sp, color = Color(0xCCFFFFFF), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onItem(item) },
                    modifier = Modifier
                        .focusRequester(heroFocus)
                        .focusProperties { up = searchFocus },
                ) { Text(heroVerb(item)) }
            }
            if (count > 1) {
                Row(Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(count) { i ->
                        Box(
                            Modifier
                                .height(5.dp)
                                .width(if (i == index) 22.dp else 5.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                .background(if (i == index) Color.White else Color(0x66FFFFFF)),
                        )
                    }
                }
            }
        }
    }
}

private fun heroVerb(item: HomeItem): String = when {
    item.defaultAction == "request" && item.requestStatus != "available" -> "Request"
    item.defaultAction == "resume" || item.playAction == "resume" -> "Resume"
    else -> "Play"
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun ClockText() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }
    val text = remember(now) { now.format(DateTimeFormatter.ofPattern("EEE, MMM d   h:mm a")) }
    Text(text, fontSize = 16.sp, color = Color(0xE6FFFFFF))
}

private enum class RailIcon { Search, Art, Apps, Settings }

/** The slim left rail: search, screen saver, apps, Lobby settings. */
@Composable
private fun Rail(
    onSearch: () -> Unit,
    onScreenSaver: () -> Unit,
    onApps: () -> Unit,
    onSettings: () -> Unit,
    searchFocus: FocusRequester,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxHeight().width(88.dp).padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RailButton(RailIcon.Search, "Search", onSearch, Modifier.focusRequester(searchFocus))
        RailButton(RailIcon.Art, "Screen saver", onScreenSaver)
        RailButton(RailIcon.Apps, "Apps", {
            onApps()
            scope.launch { listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)) }
        })
        RailButton(RailIcon.Settings, "Lobby settings", onSettings)
    }
}

@Composable
private fun RailButton(icon: RailIcon, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val tint = if (focused) Color(0xFF0E1116) else Color(0xCCFFFFFF)
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0xFFF4F2EE),
        ),
        modifier = modifier.size(52.dp).semantics { contentDescription = label },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(24.dp)) { drawRailIcon(icon, tint) }
        }
    }
}

private fun DrawScope.drawRailIcon(icon: RailIcon, tint: Color) {
    val w = size.width
    val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (icon) {
        RailIcon.Search -> {
            drawCircle(tint, radius = w * 0.32f, center = Offset(w * 0.43f, w * 0.43f), style = stroke)
            drawLine(tint, Offset(w * 0.66f, w * 0.66f), Offset(w * 0.9f, w * 0.9f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        RailIcon.Art -> {
            drawRoundRect(tint, topLeft = Offset(w * 0.08f, w * 0.14f), size = Size(w * 0.84f, w * 0.72f), cornerRadius = CornerRadius(w * 0.1f), style = stroke)
            drawCircle(tint, radius = w * 0.07f, center = Offset(w * 0.34f, w * 0.38f))
            val hills = Path().apply {
                moveTo(w * 0.12f, w * 0.78f); lineTo(w * 0.38f, w * 0.54f); lineTo(w * 0.55f, w * 0.68f); lineTo(w * 0.68f, w * 0.56f); lineTo(w * 0.88f, w * 0.76f)
            }
            drawPath(hills, tint, style = stroke)
        }
        RailIcon.Apps -> {
            val side = w * 0.32f
            for (x in listOf(0.1f, 0.58f)) for (y in listOf(0.1f, 0.58f)) {
                drawRoundRect(tint, topLeft = Offset(w * x, w * y), size = Size(side, side), cornerRadius = CornerRadius(w * 0.07f), style = stroke)
            }
        }
        RailIcon.Settings -> {
            drawCircle(tint, radius = w * 0.17f, center = Offset(w / 2, w / 2), style = stroke)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val inner = w * 0.32f
                val outer = w * 0.44f
                drawLine(
                    tint,
                    Offset(w / 2 + (inner * cos(a)).toFloat(), w / 2 + (inner * sin(a)).toFloat()),
                    Offset(w / 2 + (outer * cos(a)).toFloat(), w / 2 + (outer * sin(a)).toFloat()),
                    strokeWidth = stroke.width * 1.4f,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(tint, radius = w * 0.32f, center = Offset(w / 2, w / 2), style = stroke)
        }
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

/** First-run nudge: shown on Home until Lobby is the home app or the user says "Not now". */
@Composable
private fun HomePrompt(onSetup: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Make Lobby your home screen", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "So the Home button on your remote opens Lobby.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onSetup) { Text("Set up") }
        Button(onClick = onDismiss) { Text("Not now") }
    }
}

@Composable
internal fun MediaRow(row: HomeRow, address: ServerAddress, onItem: (HomeItem) -> Unit, onFocused: (HomeItem?) -> Unit = {}) {
    val poster = row.layout == "poster"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(row.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xCCFFFFFF))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
            items(row.items.distinctBy { it.id }, key = { it.id }) { item -> MediaCard(item, address, poster, onItem, onFocused) }
        }
    }
}

@Composable
private fun MediaCard(item: HomeItem, address: ServerAddress, poster: Boolean, onItem: (HomeItem) -> Unit, onFocused: (HomeItem?) -> Unit) {
    val width = if (poster) POSTER_W else LANDSCAPE_W
    val height = if (poster) POSTER_H else LANDSCAPE_H
    val art = address.imageUrl(if (poster) item.posterUrl ?: item.landscapeUrl else item.landscapeUrl ?: item.posterUrl, width = width * 2)

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val titleColor by animateColorAsState(if (focused) Color(0xFFF4F2EE) else Color(0x99F4F2EE), tween(140), label = "title")
    val subColor by animateColorAsState(if (focused) Color(0xB8F4F2EE) else Color(0x61F4F2EE), tween(140), label = "sub")
    val sub = item.subtitle ?: item.requestStatus?.takeIf { it != "none" }
    val progress = item.progressPercent?.takeIf { it > 0 }

    Card(onClick = { onItem(item) }, interactionSource = interaction, modifier = Modifier.width(width.dp).onFocusChanged { if (it.isFocused) onFocused(item) }) {
        Box(Modifier.fillMaxWidth().height(height.dp)) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            // The name sits inside the card over a bottom scrim: muted so a row reads at a glance, full strength on focus.
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB8000000))))
                    .padding(start = 10.dp, end = 10.dp, top = 28.dp, bottom = if (progress != null) 12.dp else 8.dp),
            ) {
                Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!sub.isNullOrBlank()) {
                    Text(sub, fontSize = 10.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            progress?.let { percent ->
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth((percent / 100.0).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(LobbyColors.accent))
                }
            }
        }
    }
}

@Composable
private fun InputRow(inputs: List<TvInput>, onInput: (TvInput) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Inputs", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xCCFFFFFF))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
            items(inputs, key = { it.id }) { input ->
                Button(onClick = { onInput(input) }) { Text(input.label, fontSize = 14.sp) }
            }
        }
    }
}

/** The app dock: a rounded bar of launchable apps along the bottom, like the Samsung app's. */
@Composable
private fun AppDock(apps: List<DockApp>, onApp: (DockApp) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(Color(0x99181C24))
            .padding(vertical = 12.dp),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(apps.distinctBy { it.packageName }, key = { it.packageName }) { app ->
                val bitmap = remember(app.packageName) { app.icon?.toBitmap(320, 180)?.asImageBitmap() }
                Card(onClick = { onApp(app) }, modifier = Modifier.width(132.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(74.dp),
                        )
                    }
                    Text(app.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp))
                }
            }
        }
    }
}
