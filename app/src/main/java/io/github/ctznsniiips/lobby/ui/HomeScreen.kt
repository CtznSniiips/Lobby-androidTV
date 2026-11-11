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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
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
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import io.github.ctznsniiips.lobby.R
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.palette.graphics.Palette
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size as CoilSize
import coil.transform.Transformation
import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.text.TextStyle
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

private const val LANDSCAPE_W = 164
private const val LANDSCAPE_H = 92
private const val POSTER_W = 84
private const val POSTER_H = 126
private const val ROTATE_MS = 8000L
// Room for a focused card to grow into without the row clipping it, especially the first card in a row.
private val ROW_H_PAD = 14.dp
private val ROW_V_PAD = 10.dp
// The hero text and greeting are inset to clear the floating rail; the rows start much further left.
private val HERO_INSET = 76.dp
private val ROWS_INSET = 16.dp

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

    val settingsFocus = remember { FocusRequester() }
    val heroArt = address.imageUrl(heroItem?.backdropUrl ?: heroItem?.landscapeUrl, width = 400)
    val accent = rememberAccentColor(heroArt)

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val wallpaper = remember(state.wallpaper) { Wallpapers.firstOrNull { it.id == state.wallpaper } ?: Wallpapers.first() }
    Box(Modifier.fillMaxSize().background(LobbyColors.background).onSizeChanged { rootSize = it }) {
        // Photo background mode: the chosen wallpaper fills the screen behind a readability scrim.
        if (state.bgMode == "photo") {
            AsyncImage(model = wallpaper.fullUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            // Readability scrim, tuned by the Material: Frost adds a soft wash, Clear tint a cooler, deeper one.
            val scrim = when (state.material) {
                "frost" -> Color(0x660C0A08)
                "tint" -> Color(0x80060A0E)
                else -> Color(0x730B0D10)
            }
            Box(Modifier.fillMaxSize().background(scrim))
        }

        HeroBackdrop(heroItem, address)

        // A soft wash of the hero image's dominant colour, kept to the upper-left so it doesn't bleed into the rows.
        if (accent != Color.Transparent) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            0f to accent.copy(alpha = 0.30f),
                            0.55f to Color.Transparent,
                            center = Offset(size.width * 0.14f, size.height * 0.24f),
                            radius = size.width * 0.42f,
                        ),
                    )
                },
            )
        }

        // Content fills the width; the rail is an overlay, so the rows reach almost to the left edge while the
        // hero text and greeting are inset enough to clear it.
        Column(Modifier.fillMaxSize().padding(end = 40.dp, top = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = HERO_INSET),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(greeting(), fontSize = 16.sp, color = LobbyColors.soft, modifier = Modifier.weight(1f))
                if (state.loading) Text("Loading…", fontSize = 14.sp, color = LobbyColors.mute, modifier = Modifier.padding(end = 20.dp))
                ClockText()
                state.weather?.let { WeatherChip(it, Modifier.padding(start = 18.dp)) }
            }

            Hero(
                item = heroItem,
                address = address,
                count = featured.size,
                index = featuredIndex,
                onItem = onItem,
                searchFocus = searchFocus,
                heroFocus = heroFocus,
                onCycle = { delta ->
                    if (featured.size <= 1) false
                    else if (delta < 0 && featuredIndex == 0) false // let focus fall left to the rail
                    else {
                        featuredIndex = (featuredIndex + delta + featured.size) % featured.size
                        true
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(start = HERO_INSET).height(164.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(start = ROWS_INSET),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 0.dp, bottom = if (hasDock) 112.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
            }
        }

        // The left rail floats over the content so the rows can run full width beneath it.
        Rail(
            onSearch = onSearch,
            onScreenSaver = onScreenSaver,
            onApps = if (hasDock) ({ runCatching { dockFocus.requestFocus() } }) else null,
            onSettings = onLobbySettings,
            searchFocus = searchFocus,
            settingsFocus = settingsFocus,
            settingsDown = if (hasDock) dockFocus else null,
            modifier = Modifier.align(Alignment.TopStart),
        )

        // The app dock stays pinned along the bottom, centred, over the rows.
        if (hasDock) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .onFocusChanged { if (it.hasFocus) browsed = null },
            ) { AppDock(state.apps, onApp, dockFocus, glass = state.bgMode == "photo", wallpaperUri = wallpaper.fullUri, material = state.material, rootSize = rootSize) }
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
        Box(
            Modifier
                .fillMaxWidth(0.68f)
                .fillMaxHeight(0.66f)
                .align(Alignment.TopEnd)
                // Fade the image's own alpha at the left and bottom edges, so it melts into whatever is behind it
                // (the page colour or a wallpaper) with no hard seam — works the same in dark, light and photo modes.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.45f to Color.White), blendMode = BlendMode.DstIn)
                    drawRect(Brush.verticalGradient(0.55f to Color.White, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                },
        ) {
            Crossfade(targetState = art, animationSpec = tween(600), label = "backdrop") { url ->
                if (url != null) {
                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun Hero(
    item: HomeItem?,
    address: ServerAddress,
    count: Int,
    index: Int,
    onItem: (HomeItem) -> Unit,
    searchFocus: FocusRequester,
    heroFocus: FocusRequester,
    onCycle: (delta: Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        if (item != null) {
            // Right side: the show's logo (title treatment), bottom-right over the backdrop, as the Samsung app
            // places it; its title text when there is no logo. Sized and positioned so it never meets the text.
            val logo = address.imageUrl(item.logoUrl, width = 640)
            when {
                logo != null -> AsyncImage(
                    model = logo,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomEnd,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).heightIn(max = 96.dp).widthIn(max = 300.dp),
                )
                !item.tagline.isNullOrBlank() -> Text(
                    item.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).widthIn(max = 300.dp),
                )
            }

            // Left side (Samsung .w-info, ~52% width so it clears the logo): tagline heading, overview, button + meta.
            Column(Modifier.align(Alignment.TopStart).fillMaxWidth(0.52f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val heading = item.tagline?.takeIf { it.isNotBlank() } ?: item.title
                Text(
                    heading,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    color = LobbyColors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val desc = item.overview?.takeIf { it.isNotBlank() }
                if (desc != null) {
                    Text(desc, fontSize = 13.sp, lineHeight = 17.sp, color = LobbyColors.soft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HeroButton(
                        text = heroVerb(item),
                        heroFocus = heroFocus,
                        searchFocus = searchFocus,
                        onCycle = onCycle,
                        onClick = { onItem(item) },
                    )
                    val details = heroDetails(item)
                    if (details.isNotBlank()) {
                        Text(details, fontSize = 12.sp, color = LobbyColors.mute, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (count > 1) {
                Row(Modifier.align(Alignment.BottomEnd).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

/**
 * The hero action button. Built on Surface with explicit padding (not the stock Button) so the label is never
 * clipped, and styled like Samsung's .wi-btn: subtle fill, solid white with dark text on focus.
 */
@Composable
private fun HeroButton(
    text: String,
    heroFocus: FocusRequester,
    searchFocus: FocusRequester,
    onCycle: (delta: Int) -> Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = LobbyColors.chromeSubtle,
            contentColor = LobbyColors.ink,
            focusedContainerColor = LobbyColors.focusFill,
            focusedContentColor = LobbyColors.focusContent,
        ),
        modifier = Modifier
            .focusRequester(heroFocus)
            .focusProperties { up = searchFocus }
            // Left/right cycle the featured titles; at the first one, Left falls through to the rail.
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) when (e.key) {
                    Key.DirectionRight -> onCycle(1)
                    Key.DirectionLeft -> onCycle(-1)
                    else -> false
                } else false
            },
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun heroVerb(item: HomeItem): String = when {
    item.defaultAction == "request" && item.requestStatus != "available" -> "Request"
    item.defaultAction == "resume" || item.playAction == "resume" -> "Resume"
    else -> "Play"
}

/** The detail line beside the hero button: why it's featured, plus its metadata (year · runtime · genres). */
private fun heroDetails(item: HomeItem): String {
    val reason = when (item.featureReason) {
        "game" -> "Your team"
        "resume" -> "Continue watching"
        "nextup" -> "Up next"
        "new" -> "New in your library"
        "trending" -> "Trending"
        else -> null
    }
    return listOfNotNull(reason, item.meta?.takeIf { it.isNotBlank() }).joinToString("  ·  ")
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
    Text(text, fontSize = 16.sp, color = LobbyColors.ink)
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
    settingsFocus: FocusRequester,
    settingsDown: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.width(60.dp).padding(start = 8.dp, top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RailButton(RailIcon.Search, "Search", onSearch, Modifier.focusRequester(searchFocus))
        RailButton(RailIcon.Art, "Screensaver", onScreenSaver)
        if (onApps != null) RailButton(RailIcon.Apps, "Go to apps", onApps)
        RailButton(
            RailIcon.Settings,
            "Lobby Settings",
            onSettings,
            Modifier.focusRequester(settingsFocus).then(
                if (settingsDown != null) Modifier.focusProperties { down = settingsDown } else Modifier,
            ),
        )
    }
}

/** Current weather beside the clock: the Samsung weather glyph (as a vector drawable) and the temperature. */
@Composable
private fun WeatherChip(weather: WeatherNow, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            painter = painterResource(weatherIconRes(weather.code, weather.day)),
            contentDescription = null,
            tint = LobbyColors.ink,
            modifier = Modifier.size(22.dp),
        )
        Text(weather.text, fontSize = 16.sp, color = LobbyColors.ink)
    }
}

/** WMO weather code → the matching drawable, using the Samsung app's own mapping. */
private fun weatherIconRes(code: Int, day: Boolean): Int = when {
    code == 0 -> if (day) R.drawable.wx_sun else R.drawable.wx_moon
    code == 1 || code == 2 -> if (day) R.drawable.wx_partly else R.drawable.wx_cloud
    code == 3 -> R.drawable.wx_cloud
    code == 45 || code == 48 -> R.drawable.wx_fog
    (code in 71..77) || code == 85 || code == 86 -> R.drawable.wx_snow
    code >= 95 -> R.drawable.wx_storm
    code in 51..82 -> R.drawable.wx_rain
    else -> R.drawable.wx_cloud
}

@Composable
private fun RailButton(icon: RailIcon, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val tint = if (focused) LobbyColors.focusContent else LobbyColors.soft
    Box {
        val glassFocus = LobbyColors.focusStyle == "glass"
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                // In Glass mode the plate is the custom 3D selector below; in Type mode it's the solid fill.
                focusedContainerColor = if (glassFocus) Color.Transparent else LobbyColors.focusFill,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
            modifier = modifier.size(34.dp).semantics { contentDescription = label },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (focused && glassFocus) GlassSelector(androidx.compose.foundation.shape.CircleShape)
                Canvas(Modifier.size(15.dp)) { drawRailIcon(icon, tint) }
            }
        }
        // On focus, a label pill appears to the right of the icon. A Popup lets it overflow the narrow rail
        // without widening it or shifting the icons.
        if (focused) {
            val gap = with(LocalDensity.current) { 44.dp.roundToPx() }
            Popup(alignment = Alignment.CenterStart, offset = IntOffset(gap, 0), properties = PopupProperties(focusable = false)) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LobbyColors.background,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(LobbyColors.ink)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
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

/** Loads a small, software-blurred copy of the wallpaper as an ImageBitmap, for drawing an aligned slice behind the dock. */
@Composable
private fun rememberBlurredWallpaper(uri: String): ImageBitmap? {
    val context = LocalContext.current
    var bmp by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        // A sharp, high-detail decode; the clean Gaussian blur is applied at draw time, so there's no downscale
        // blockiness. The Material sets the blur radius.
        val request = ImageRequest.Builder(context).data(uri).allowHardware(false).size(1280).build()
        val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
        bmp = (drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
    }
    return bmp
}

@Composable
internal fun MediaRow(row: HomeRow, address: ServerAddress, onItem: (HomeItem) -> Unit, onFocused: (HomeItem?) -> Unit = {}) {
    val poster = row.layout == "poster"
    // Samsung brightens a row's title only while that row is active (holds the focused card): ~95% vs ~58% ink.
    var active by remember { mutableStateOf(false) }
    val titleColor by animateColorAsState(if (active) LobbyColors.ink else LobbyColors.mute, tween(140), label = "rowTitle")
    Column(
        Modifier.onFocusChanged { active = it.hasFocus },
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            row.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp,
            color = titleColor,
            modifier = Modifier.padding(start = ROW_H_PAD),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            // Small top gap keeps the title close to its cards; the bottom gap gives a focused card room to scale.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = ROW_H_PAD, end = ROW_H_PAD, top = 3.dp, bottom = ROW_V_PAD),
        ) {
            items(row.items.distinctBy { it.id }, key = { it.id }) { item -> MediaCard(item, address, poster, row.id == "seerr-trending", onItem, onFocused) }
        }
    }
}

@Composable
private fun MediaCard(item: HomeItem, address: ServerAddress, poster: Boolean, trending: Boolean, onItem: (HomeItem) -> Unit, onFocused: (HomeItem?) -> Unit) {
    val width = if (poster) POSTER_W else LANDSCAPE_W
    val height = if (poster) POSTER_H else LANDSCAPE_H
    val art = address.imageUrl(if (poster) item.posterUrl ?: item.landscapeUrl else item.landscapeUrl ?: item.posterUrl, width = width * 2)

    // Discover rows (Trending, Popular Movies, Popular Shows) use posters that already carry the title.
    val discover = item.kind == "discover"
    val logo = address.imageUrl(item.logoUrl, width = 240)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val titleColor by animateColorAsState(if (focused) Color(0xFFF4F2EE) else Color(0x99F4F2EE), tween(140), label = "title")
    val progress = item.progressPercent?.takeIf { it > 0 }
    val sub = item.subtitle ?: item.meta?.takeIf { discover }

    // Top-left badge. For a game (program): "Live" in red when airing, else its start time. Otherwise the
    // Seerr/library status if the title has one; or, in the Trending row only, its type.
    val programBadge: Pair<String, Color>? = if (item.kind == "program") {
        if (item.isAiring == true) "Live" to Color(0xFFC8372D)
        else gameTimeText(item.startDate)?.let { it to Color(0xB80A0C0E) }
    } else {
        null
    }
    val status = item.requestStatus?.takeIf { it != "none" }?.takeIf { discover }
    val typeBadge = if (trending && status == null && item.mediaType != null) {
        if (item.mediaType == "tv") "Series" else "Movie"
    } else {
        null
    }

    // Samsung behaviour: the show's logo sits bottom-left on the card (muted title if it has no logo); the detail
    // line shows below the card only while focused (a fixed slot is reserved so the row doesn't jump).
    Column(Modifier.width(width.dp)) {
        Card(
            onClick = { onItem(item) },
            interactionSource = interaction,
            scale = CardDefaults.scale(focusedScale = 1.04f),
            border = CardDefaults.border(
                focusedBorder = Border(BorderStroke(1.5.dp, Color(0x9EFFFFFF)), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
            ),
            modifier = Modifier.onFocusChanged { if (it.isFocused) onFocused(item) },
        ) {
            Box(Modifier.fillMaxWidth().height(height.dp)) {
                if (art != null) {
                    AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                // The discovery rows show no on-card title (the poster already has it).
                if (!discover) {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xA6000000))))
                            .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = if (progress != null) 10.dp else 7.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        if (logo != null) {
                            AsyncImage(
                                model = logo,
                                contentDescription = item.title,
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.BottomStart,
                                modifier = Modifier.heightIn(max = 22.dp).widthIn(max = (width * 0.74f).dp),
                            )
                        } else {
                            Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Status, program-time, or type badge, top-left.
                val badgeMax = (width - 12).dp
                when {
                    programBadge != null -> CardBadge(programBadge.first, programBadge.second, badgeMax, Modifier.align(Alignment.TopStart))
                    status != null -> CardBadge(statusLabel(status), statusColor(status), badgeMax, Modifier.align(Alignment.TopStart))
                    typeBadge != null -> CardBadge(typeBadge, Color(0xB80A0C0E), badgeMax, Modifier.align(Alignment.TopStart))
                }

                progress?.let { percent ->
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x55FFFFFF))) {
                        Box(Modifier.fillMaxWidth((percent / 100.0).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(LobbyColors.accent))
                    }
                }
            }
        }
        // Discovery cards show nothing below (and so pack their rows closer); other cards reserve a detail slot.
        if (!discover) {
            Box(Modifier.fillMaxWidth().height(16.dp).padding(top = 3.dp, start = 2.dp)) {
                if (focused && !sub.isNullOrBlank()) {
                    Text(sub, fontSize = 10.sp, color = LobbyColors.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** A small rounded badge in the top-left of a card (status or type), sized like Samsung's .wc .mc-badge. */
@Composable
private fun CardBadge(text: String, background: Color, maxWidth: Dp, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(start = 5.dp, top = 5.dp)
            .widthIn(max = maxWidth)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
            .background(background)
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
    )
}

/** A program's start time formatted like the Samsung app ("7:00 pm"), in the device's zone. Null if unparseable. */
private fun gameTimeText(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val zone = java.time.ZoneId.systemDefault()
    val time = runCatching { java.time.OffsetDateTime.parse(iso).atZoneSameInstant(zone) }.getOrNull()
        ?: runCatching { java.time.Instant.parse(iso).atZone(zone) }.getOrNull()
        ?: runCatching { java.time.LocalDateTime.parse(iso).atZone(zone) }.getOrNull()
        ?: return null
    return time.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
}

/** Seerr/library status → label and colour, matching the Samsung app's badge set. */
private fun statusLabel(status: String): String = when (status) {
    "available" -> "Available"
    "partial" -> "Partly available"
    "processing" -> "Processing"
    "requested" -> "Requested"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun statusColor(status: String): Color = when (status) {
    "available" -> Color(0xEB207848)
    "partial" -> Color(0xEB206082)
    "processing", "requested" -> Color(0xEB785214)
    else -> Color(0xB80A0C0E)
}

@Composable
private fun InputRow(inputs: List<TvInput>, onInput: (TvInput) -> Unit) {
    var active by remember { mutableStateOf(false) }
    val titleColor by animateColorAsState(if (active) LobbyColors.ink else LobbyColors.mute, tween(140), label = "inputsTitle")
    Column(
        Modifier.onFocusChanged { active = it.hasFocus },
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text("Inputs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp, color = titleColor, modifier = Modifier.padding(start = ROW_H_PAD))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ROW_H_PAD, vertical = ROW_V_PAD)) {
            items(inputs, key = { it.id }) { input ->
                Button(onClick = { onInput(input) }) { Text(input.label, fontSize = 14.sp) }
            }
        }
    }
}

/**
 * Blurs a bitmap in software by shrinking it and scaling it back up with bilinear filtering. Baking the blur into
 * the pixels (rather than Modifier.blur, which only works on API 31+) makes the frosted dock blur on every Android
 * version, including older TV boxes.
 */
private class DownscaleBlur(private val factor: Int = 20) : Transformation {
    override val cacheKey: String = "DownscaleBlur($factor)"
    override suspend fun transform(input: Bitmap, size: CoilSize): Bitmap {
        val w = (input.width / factor).coerceAtLeast(2)
        val h = (input.height / factor).coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(input, w, h, true)
        val blurred = Bitmap.createScaledBitmap(small, input.width.coerceAtLeast(2), input.height.coerceAtLeast(2), true)
        if (small != blurred) small.recycle()
        return blurred
    }
}

/**
 * A raised frosted-glass focus selector. A soft drop shadow lifts it off the surface, a top-lit gradient fill gives
 * it a convex 3D feel, and a gradient rim (brighter at the top, fading down) softens the edge. Shared by the dock
 * apps and, in Glass mode, the free rail icons.
 */
@Composable
private fun BoxScope.GlassSelector(shape: Shape) {
    Box(
        Modifier.matchParentSize()
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x2EFFFFFF))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x85FFFFFF), Color(0x1AFFFFFF))), shape),
    )
}

/** The app dock: a compact, centred rounded bar of launchable apps pinned at the bottom, like the Samsung app's. */
@Composable
private fun AppDock(
    apps: List<DockApp>,
    onApp: (DockApp) -> Unit,
    firstFocus: FocusRequester,
    glass: Boolean,
    wallpaperUri: String,
    material: String,
    rootSize: IntSize,
) {
    val list = remember(apps) { apps.distinctBy { it.packageName } }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val context = LocalContext.current
    // A heavily downscaled decode of the wallpaper: scaled back up it reads as a soft blur on EVERY Android
    // version (Modifier.blur only blurs on API 31+), which is what makes the dock look like frosted glass.
    // A fallback blurred fill (used until the aligned slice is ready), baked into the bitmap so it blurs everywhere.
    val blurModel = remember(wallpaperUri) {
        ImageRequest.Builder(context).data(wallpaperUri).transformations(DownscaleBlur(22)).allowHardware(false).build()
    }
    // How frosted each Material is, as a clean Gaussian radius: Clear tint stays sharp, Soft a gentle frost, Frost heavy.
    val blurRadius: Dp = when (material) {
        "tint" -> 1.dp
        "frost" -> 42.dp
        else -> 24.dp
    }
    // The sharp wallpaper copy whose aligned slice is drawn behind the dock for the true "window" look.
    val blurredBmp = if (glass) rememberBlurredWallpaper(wallpaperUri) else null
    var panelPos by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            // A soft drop shadow lets the bar float instead of reading as a hard cut-out.
            .shadow(24.dp, shape, clip = false)
            .clip(shape)
            // A dim rim, softer than before, following the rounded shape.
            .border(1.dp, Color(0x24FFFFFF), shape)
            .then(if (glass) Modifier.onGloballyPositioned { panelPos = it.positionInWindow() } else Modifier),
    ) {
        // Backdrop layer. matchParentSize keeps it from inflating the bar — only the tile Row below sizes the dock.
        Box(Modifier.matchParentSize().clip(shape)) {
            if (glass) {
                if (blurredBmp != null && rootSize.width > 0) {
                    // Draw the exact slice of the wallpaper that sits behind the dock, replicating the background's
                    // ContentScale.Crop, then apply a clean Gaussian blur — a true window onto the frosted wallpaper.
                    // Clear tint uses a 0dp radius (no blur): the wallpaper shows through sharp.
                    Canvas(
                        Modifier.matchParentSize()
                            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded) else Modifier),
                    ) {
                        val cw = size.width
                        val ch = size.height
                        val sw = rootSize.width.toFloat()
                        val sh = rootSize.height.toFloat()
                        val iw = blurredBmp.width.toFloat()
                        val ih = blurredBmp.height.toFloat()
                        val imageAspect = iw / ih
                        val screenAspect = sw / sh
                        var visW = 1f
                        var visH = 1f
                        var left = 0f
                        var top = 0f
                        if (screenAspect >= imageAspect) { visH = imageAspect / screenAspect; top = (1f - visH) / 2f }
                        else { visW = screenAspect / imageAspect; left = (1f - visW) / 2f }
                        val fx = (panelPos.x / sw).coerceIn(0f, 1f)
                        val fy = (panelPos.y / sh).coerceIn(0f, 1f)
                        val fw = (cw / sw).coerceIn(0f, 1f)
                        val fh = (ch / sh).coerceIn(0f, 1f)
                        val srcL = (left + fx * visW) * iw
                        val srcT = (top + fy * visH) * ih
                        val srcW = (fw * visW * iw).coerceAtLeast(1f)
                        val srcH = (fh * visH * ih).coerceAtLeast(1f)
                        drawImage(
                            image = blurredBmp,
                            srcOffset = IntOffset(srcL.roundToInt(), srcT.roundToInt()),
                            srcSize = IntSize(srcW.roundToInt(), srcH.roundToInt()),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(cw.roundToInt(), ch.roundToInt()),
                        )
                    }
                } else {
                    AsyncImage(
                        model = blurModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                // Material-matched veil over the blurred wallpaper (dark neutral glass, as in the reference). Clear
                // tint stays the most transparent; Soft a medium darkening; Frost the deepest, coolest frost.
                val veil = when (material) {
                    "frost" -> Color(0x52FFFFFF)
                    "tint" -> Color(0x470A0E14)
                    else -> Color(0x730B0D10)
                }
                Box(Modifier.matchParentSize().background(veil))
            } else {
                Box(Modifier.matchParentSize().background(Color(0xD90F1319)))
            }
        }
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        list.forEachIndexed { i, app ->
            val bitmap = remember(app.packageName) { app.icon?.toBitmap(240, 135)?.asImageBitmap() }
            val tileInteraction = remember { MutableInteractionSource() }
            val tileFocused by tileInteraction.collectIsFocusedAsState()
            val tileShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            Surface(
                onClick = { onApp(app) },
                interactionSource = tileInteraction,
                shape = ClickableSurfaceDefaults.shape(tileShape),
                colors = ClickableSurfaceDefaults.colors(
                    // The focused plate is drawn as a custom 3D glass selector below, so the surface stays clear.
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                modifier = Modifier.width(88.dp).then(if (i == 0) Modifier.focusRequester(firstFocus) else Modifier),
            ) {
                // fillMaxWidth (not fillMaxSize): in the wrap-height dock Row, fillMaxSize would stretch the tile to
                // the whole screen. The Column sizes the height; the selector plate matches via matchParentSize.
                Box(Modifier.fillMaxWidth()) {
                    if (tileFocused) GlassSelector(tileShape)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    // Fill a rounded tile with the banner so the icon's own corners are rounded off.
                    Box(
                        Modifier
                            .width(52.dp)
                            .height(28.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                            .background(Color(0x22000000)),
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = app.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    // Matches Samsung's .wapp-name: SemiBold, soft ink. A soft shadow keeps it legible on bright frost.
                    Text(
                        app.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LobbyColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = Shadow(Color(0x99000000), blurRadius = 8f)),
                    )
                }
                }
            }
        }
        }
    }
}
