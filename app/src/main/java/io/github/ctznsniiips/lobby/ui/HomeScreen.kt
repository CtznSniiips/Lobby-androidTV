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
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import androidx.tv.material3.CardDefaults
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import io.github.ctznsniiips.lobby.data.WeatherNow
import io.github.ctznsniiips.lobby.device.DockApp
import io.github.ctznsniiips.lobby.device.TvInput
import kotlinx.coroutines.delay

private const val LANDSCAPE_W = 188
private const val LANDSCAPE_H = 106
private const val POSTER_W = 92
private const val POSTER_H = 138
private const val ROTATE_MS = 8000L
// Room for a focused card to grow into without the row clipping it, especially the first card in a row.
private val ROW_H_PAD = 14.dp
private val ROW_V_PAD = 12.dp

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
    val dockFocus = remember { FocusRequester() }
    val hasDock = state.apps.isNotEmpty()

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

    val heroArt = address.imageUrl(heroItem?.backdropUrl ?: heroItem?.landscapeUrl, width = 400)
    val accent = rememberAccentColor(heroArt)

    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        HeroBackdrop(heroItem, address)

        // A soft wash of the hero image's dominant colour on the left, like the Samsung app.
        if (accent != Color.Transparent) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            0f to accent.copy(alpha = 0.34f),
                            0.62f to Color.Transparent,
                            center = Offset(size.width * 0.16f, size.height * 0.52f),
                            radius = size.width * 0.6f,
                        ),
                    )
                },
            )
        }

        Row(Modifier.fillMaxSize()) {
            Rail(
                onSearch = onSearch,
                onScreenSaver = onScreenSaver,
                onApps = if (hasDock) ({ runCatching { dockFocus.requestFocus() } }) else null,
                onSettings = onLobbySettings,
                searchFocus = searchFocus,
            )

            Column(Modifier.fillMaxSize().padding(end = 40.dp, top = 24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(greeting(), fontSize = 16.sp, color = LobbyColors.soft, modifier = Modifier.weight(1f))
                    if (state.loading) Text("Loading…", fontSize = 14.sp, color = LobbyColors.mute, modifier = Modifier.padding(end = 20.dp))
                    ClockText()
                    state.weather?.let { WeatherChip(it, Modifier.padding(start = 18.dp)) }
                }

                Hero(heroItem, featured.size, featuredIndex, onItem, searchFocus, heroFocus, Modifier.fillMaxWidth().height(168.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = if (hasDock) 128.dp else 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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

                    home?.rows?.let { mergeContinueRows(it) }?.filter { it.items.isNotEmpty() }?.let { rows ->
                        items(rows, key = { it.id }) { row ->
                            MediaRow(row, address, onItem, onFocused = { browsed = it })
                        }
                    }

                    if (state.inputs.isNotEmpty()) {
                        item { Box(Modifier.onFocusChanged { if (it.hasFocus) browsed = null }) { InputRow(state.inputs, onInput) } }
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

        // The app dock stays pinned along the bottom, centred, over the rows.
        if (hasDock) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
                    .onFocusChanged { if (it.hasFocus) browsed = null },
            ) { AppDock(state.apps, onApp, dockFocus) }
        }

        MessageToast(state.message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HeroBackdrop(item: HomeItem?, address: ServerAddress) {
    val art = address.imageUrl(item?.backdropUrl ?: item?.landscapeUrl, width = 1600)
    // The image lives in the upper-right of the screen and dissolves into the page toward the left and the bottom,
    // the way the Samsung app frames it — the rows and hero text sit over flat background, not over the picture.
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth(0.68f).fillMaxHeight(0.66f).align(Alignment.TopEnd)) {
            Crossfade(targetState = art, animationSpec = tween(600), label = "backdrop") { url ->
                if (url != null) {
                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            // Fade the image out to the left so it blends into the dark text area.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(0f to LobbyColors.background, 0.42f to Color(0x730E1116), 1f to Color.Transparent),
                ),
            )
            // And fade it down into the rows below.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 0.62f to Color(0x8C0E1116), 1f to LobbyColors.background),
                ),
            )
        }
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
                Text(item.title, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val line = item.tagline ?: item.meta ?: item.subtitle
                if (!line.isNullOrBlank()) {
                    Text(line, fontSize = 16.sp, color = LobbyColors.soft, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    onApps: (() -> Unit)?,
    onSettings: () -> Unit,
    searchFocus: FocusRequester,
) {
    Column(
        Modifier.fillMaxHeight().width(88.dp).padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RailButton(RailIcon.Search, "Search", onSearch, Modifier.focusRequester(searchFocus))
        RailButton(RailIcon.Art, "Screen saver", onScreenSaver)
        if (onApps != null) RailButton(RailIcon.Apps, "Apps", onApps)
        RailButton(RailIcon.Settings, "Lobby settings", onSettings)
    }
}

/** Current weather beside the clock: a small drawn icon and the temperature. */
@Composable
private fun WeatherChip(weather: WeatherNow, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(22.dp)) { drawWeatherIcon(weather.code, weather.day, Color(0xE6FFFFFF)) }
        Text(weather.text, fontSize = 16.sp, color = Color(0xE6FFFFFF))
    }
}

/** Minimal line-art weather glyphs keyed off the WMO code, matching the Samsung app's set. */
private fun DrawScope.drawWeatherIcon(code: Int, day: Boolean, tint: Color) {
    val w = size.width
    val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun cloud(cx: Float, cy: Float, s: Float) {
        drawRoundRect(tint, topLeft = Offset(cx - s, cy - s * 0.4f), size = Size(s * 2f, s * 0.9f), cornerRadius = CornerRadius(s * 0.5f), style = stroke)
        drawCircle(tint, radius = s * 0.55f, center = Offset(cx - s * 0.4f, cy - s * 0.35f), style = stroke)
        drawCircle(tint, radius = s * 0.7f, center = Offset(cx + s * 0.35f, cy - s * 0.4f), style = stroke)
    }
    fun sun(cx: Float, cy: Float, r: Float) {
        drawCircle(tint, radius = r, center = Offset(cx, cy), style = stroke)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            drawLine(tint, Offset(cx + (r * 1.5f * cos(a)).toFloat(), cy + (r * 1.5f * sin(a)).toFloat()), Offset(cx + (r * 2.1f * cos(a)).toFloat(), cy + (r * 2.1f * sin(a)).toFloat()), strokeWidth = stroke.width)
        }
    }
    when {
        code == 0 && day -> sun(w * 0.5f, w * 0.5f, w * 0.2f)
        code == 0 -> drawPath(Path().apply { // moon
            moveTo(w * 0.72f, w * 0.6f); cubicTo(w * 0.5f, w * 0.62f, w * 0.38f, w * 0.44f, w * 0.5f, w * 0.26f); cubicTo(w * 0.3f, w * 0.3f, w * 0.26f, w * 0.6f, w * 0.46f, w * 0.72f); cubicTo(w * 0.58f, w * 0.78f, w * 0.68f, w * 0.72f, w * 0.72f, w * 0.6f); close()
        }, tint, style = stroke)
        code in 1..2 && day -> { sun(w * 0.36f, w * 0.36f, w * 0.14f); cloud(w * 0.58f, w * 0.64f, w * 0.22f) }
        code in 95..99 -> { cloud(w * 0.5f, w * 0.44f, w * 0.26f); drawPath(Path().apply { moveTo(w * 0.52f, w * 0.6f); lineTo(w * 0.4f, w * 0.8f); lineTo(w * 0.54f, w * 0.8f); lineTo(w * 0.46f, w * 0.96f) }, tint, style = stroke) }
        (code in 71..77) || code == 85 || code == 86 -> { cloud(w * 0.5f, w * 0.42f, w * 0.26f); for (x in listOf(0.36f, 0.5f, 0.64f)) drawCircle(tint, radius = w * 0.025f, center = Offset(w * x, w * 0.82f)) }
        (code in 51..67) || (code in 80..82) -> { cloud(w * 0.5f, w * 0.42f, w * 0.26f); for (x in listOf(0.38f, 0.52f, 0.66f)) drawLine(tint, Offset(w * x, w * 0.68f), Offset(w * (x - 0.04f), w * 0.86f), strokeWidth = stroke.width) }
        else -> cloud(w * 0.5f, w * 0.52f, w * 0.28f)
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

/** Fold the server's Continue Watching and Next Up rows into one, in-progress titles first (as the Samsung app does). */
private fun mergeContinueRows(rows: List<HomeRow>): List<HomeRow> {
    val hasBoth = rows.any { it.id == "continue" } && rows.any { it.id == "nextup" }
    if (!hasBoth) return rows
    val out = ArrayList<HomeRow>(rows.size)
    var slot = -1
    val items = linkedMapOf<String, HomeItem>()
    for (row in rows) {
        if (row.id == "continue" || row.id == "nextup") {
            if (slot < 0) { slot = out.size; out.add(row) }
            row.items.forEach { items.putIfAbsent(it.id, it) }
        } else {
            out.add(row)
        }
    }
    val ordered = items.values.sortedByDescending { it.defaultAction == "resume" || it.playAction == "resume" }
    out[slot] = HomeRow(id = "continue", title = "Continue Watching", layout = rows.first { it.id == "continue" }.layout, items = ordered)
    return out
}

/** The dominant colour of the hero image, for the left-side wash. Transparent until it loads (or if it can't). */
@Composable
private fun rememberAccentColor(url: String?): Color {
    val context = LocalContext.current
    var color by remember { mutableStateOf(Color.Transparent) }
    LaunchedEffect(url) {
        color = Color.Transparent
        if (url.isNullOrBlank()) return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(160).build()
        val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
        val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
        swatch?.let { color = Color(it.rgb) }
    }
    return color
}

@Composable
internal fun MediaRow(row: HomeRow, address: ServerAddress, onItem: (HomeItem) -> Unit, onFocused: (HomeItem?) -> Unit = {}) {
    val poster = row.layout == "poster"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            row.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.5).sp,
            color = LobbyColors.soft,
            modifier = Modifier.padding(start = ROW_H_PAD),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            // Padding on both axes gives a focused card (which scales up) room before the row clips it.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ROW_H_PAD, vertical = ROW_V_PAD),
        ) {
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

    Card(
        onClick = { onItem(item) },
        interactionSource = interaction,
        scale = CardDefaults.scale(focusedScale = 1.06f),
        modifier = Modifier.width(width.dp).onFocusChanged { if (it.isFocused) onFocused(item) },
    ) {
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

/** The app dock: a compact, centred rounded bar of launchable apps pinned at the bottom, like the Samsung app's. */
@Composable
private fun AppDock(apps: List<DockApp>, onApp: (DockApp) -> Unit, firstFocus: FocusRequester) {
    val list = remember(apps) { apps.distinctBy { it.packageName } }
    Row(
        Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
            .background(Color(0xD90F1319))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        list.forEachIndexed { i, app ->
            val bitmap = remember(app.packageName) { app.icon?.toBitmap(240, 135)?.asImageBitmap() }
            Surface(
                onClick = { onApp(app) },
                shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color(0x26FFFFFF),
                ),
                modifier = Modifier.width(96.dp).then(if (i == 0) Modifier.focusRequester(firstFocus) else Modifier),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                        )
                    }
                    Text(app.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
