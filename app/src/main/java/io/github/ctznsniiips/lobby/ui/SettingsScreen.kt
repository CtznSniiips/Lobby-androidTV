@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import io.github.ctznsniiips.lobby.data.ServerAddress
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import io.github.ctznsniiips.lobby.data.GuideTeam
import io.github.ctznsniiips.lobby.data.TvRowSetting
import io.github.ctznsniiips.lobby.data.TvSettingsResponse
import io.github.ctznsniiips.lobby.device.DockApp
import io.github.ctznsniiips.lobby.device.TvInput

/** The settings sub-menus, matching the Samsung app's top-level groups (minus its Frame-specific appearance ones). */
private enum class SettingsCategory(val title: String) {
    Background("Background"),
    Layout("Layout"),
    Focus("Focus"),
    Typography("Typography"),
    Saver("Screen saver"),
    Apps("Apps on Home"),
    Inputs("Inputs on Home"),
    Media("Emby/Jellyfin & Seerr"),
    System("System"),
}

/**
 * This TV's own settings, kept on the server by the plugin. The landing page lists categories; opening one shows
 * just that group's controls. Changes apply on screen at once and are saved a moment later. Back steps out one
 * level at a time.
 */
@Composable
fun SettingsScreen(
    homeState: LobbyState,
    address: ServerAddress,
    settings: SettingsState,
    screenSaverMinutes: Int,
    isDefaultHome: Boolean,
    apps: List<DockApp>,
    inputs: List<TvInput>,
    hiddenApps: Set<String>,
    hiddenInputs: Set<String>,
    tvName: String,
    host: String,
    homeFont: String,
    bgMode: String,
    wallpaper: String,
    focusStyle: String,
    material: String,
    message: String?,
    actions: SettingsActions,
    onMessageShown: () -> Unit,
) {
    var category by remember { mutableStateOf<SettingsCategory?>(null) }

    // Back closes an open sub-menu before leaving settings (the teams picker has its own handler in MainActivity).
    BackHandler(enabled = category != null && !settings.teamsOpen) { category = null }

    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        when {
            settings.teamsOpen -> TeamsPicker(settings, actions)
            category == null -> CategoryList(settings, apps, inputs, hiddenApps, homeFont, bgMode, focusStyle, material, homeState.homeLayout, homeState.appDisplay, actions) { category = it }
            else -> CategoryDetail(category!!, homeState, address, settings, screenSaverMinutes, isDefaultHome, apps, inputs, hiddenApps, hiddenInputs, tvName, host, homeFont, bgMode, wallpaper, focusStyle, material, actions) { category = null }
        }
        MessageToast(message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CategoryList(
    settings: SettingsState,
    apps: List<DockApp>,
    inputs: List<TvInput>,
    hiddenApps: Set<String>,
    homeFont: String,
    bgMode: String,
    focusStyle: String,
    material: String,
    homeLayout: String,
    appDisplay: String,
    actions: SettingsActions,
    onOpen: (SettingsCategory) -> Unit,
) {
    val data = settings.data
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 56.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Lobby settings", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = LobbyColors.ink)
                val sub: String = settings.error ?: when {
                    data == null -> "Loading…"
                    data.customized -> "This TV has its own settings."
                    else -> "Using the server's defaults until you change something."
                }
                Text(sub, fontSize = 16.sp, color = if (settings.error != null) LobbyColors.error else LobbyColors.soft)
            }
        }

        val shown = SettingsCategory.entries.filter { cat ->
            when (cat) {
                SettingsCategory.Apps -> apps.isNotEmpty()
                SettingsCategory.Inputs -> inputs.isNotEmpty()
                else -> true
            }
        }
        items(shown, key = { it.name }) { cat ->
            val summary = when (cat) {
                SettingsCategory.Background -> BackgroundModes.firstOrNull { it.first == bgMode }?.second ?: "Dark"
                SettingsCategory.Layout -> {
                    val name = LayoutChoices.firstOrNull { it.first == homeLayout }?.second ?: "Watch"
                    val display = AppDisplayChoices.firstOrNull { it.first == appDisplay }?.second ?: "Icons and text"
                    "$name · $display"
                }
                SettingsCategory.Focus -> {
                    val style = FocusChoices.firstOrNull { it.first == focusStyle }?.second ?: "Type"
                    if (materialApplies(bgMode)) "$style · ${MaterialChoices.firstOrNull { it.first == material }?.second ?: "Soft"}" else style
                }
                SettingsCategory.Typography -> FontChoices.firstOrNull { it.first == homeFont }?.second ?: "Manrope"
                SettingsCategory.Saver -> "Screen saver and when it starts"
                SettingsCategory.Apps -> "${apps.count { it.packageName !in hiddenApps }} of ${apps.size} apps shown"
                SettingsCategory.Inputs -> "${inputs.size} ${if (inputs.size == 1) "input" else "inputs"}"
                SettingsCategory.Media -> data?.let { if (it.customized) "Rows, playback and teams · this TV's own settings" else "Rows, playback, teams and connection" } ?: "Loading…"
                SettingsCategory.System -> "System settings and the home screen"
            }
            CategoryCard(cat.title, summary, onClick = { onOpen(cat) })
        }
        item { Button(onClick = actions.back) { Text("Done") } }
    }
}

@Composable
private fun CategoryCard(title: String, summary: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(containerColor = LobbyColors.card, focusedContainerColor = LobbyColors.card),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LobbyColors.ink)
                if (summary.isNotBlank()) Text(summary, fontSize = 14.sp, color = LobbyColors.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("›", fontSize = 26.sp, color = LobbyColors.mute)
        }
    }
}

@Composable
private fun CategoryDetail(
    category: SettingsCategory,
    homeState: LobbyState,
    address: ServerAddress,
    settings: SettingsState,
    screenSaverMinutes: Int,
    isDefaultHome: Boolean,
    apps: List<DockApp>,
    inputs: List<TvInput>,
    hiddenApps: Set<String>,
    hiddenInputs: Set<String>,
    tvName: String,
    host: String,
    homeFont: String,
    bgMode: String,
    wallpaper: String,
    focusStyle: String,
    material: String,
    actions: SettingsActions,
    onBack: () -> Unit,
) {
    val data = settings.data
    // Appearance menus show a live preview of Home beside their choices, as in the Samsung version.
    val showPreview = category == SettingsCategory.Background ||
        category == SettingsCategory.Layout ||
        category == SettingsCategory.Focus ||
        category == SettingsCategory.Typography
    Row(Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier.weight(if (showPreview) 0.52f else 1f).fillMaxHeight(),
        contentPadding = PaddingValues(horizontal = 56.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                Text(category.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = LobbyColors.ink)
                val hint = when (category) {
                    SettingsCategory.Apps -> "Which apps appear in the dock at the bottom of Home, and in what order."
                    SettingsCategory.Inputs -> "Which TV inputs appear on Home."
                    SettingsCategory.Focus -> "How the highlight looks when you move around Home. Material tunes the glass over a photo background."
                    SettingsCategory.Layout -> "How Home is arranged, and how your apps are labelled. Watch leads with the hero; Dock drops it for an open top; List runs your apps down the left."
                    else -> null
                }
                if (hint != null) Text(hint, fontSize = 15.sp, color = LobbyColors.soft)
            }
        }

        when (category) {
            SettingsCategory.Apps -> items(apps, key = { it.packageName }) { app ->
                val index = apps.indexOfFirst { it.packageName == app.packageName }
                AppLine(
                    label = app.label,
                    hidden = app.packageName in hiddenApps,
                    canMoveUp = index > 0,
                    canMoveDown = index < apps.lastIndex,
                    onToggle = { hide -> actions.setAppHidden(app.packageName, hide) },
                    onMove = { delta -> actions.moveApp(app.packageName, delta) },
                )
            }
            SettingsCategory.Inputs -> items(inputs, key = { it.id }) { input ->
                ToggleLine(input.label, hidden = input.id in hiddenInputs, onLabel = "On Home", offLabel = "Add to Home") { hide ->
                    actions.setInputHidden(input.id, hide)
                }
            }
            SettingsCategory.Background -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BackgroundModes.forEach { (mode, name) ->
                            Button(onClick = { actions.setBgMode(mode) }) {
                                Text(name)
                                if (mode == bgMode) Text("  •", color = LobbyColors.accent)
                            }
                        }
                    }
                }
                if (bgMode == "photo") {
                    item { SectionHeader("Wallpaper") }
                    items(Wallpapers.chunked(3), key = { it.first().id }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { wp ->
                                WallpaperTile(wp, selected = wp.id == wallpaper, modifier = Modifier.weight(1f)) { actions.setWallpaper(wp.id) }
                            }
                            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            SettingsCategory.Layout -> {
                item { SectionHeader("Home layout") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LayoutChoices.forEach { (id, name) ->
                            Button(onClick = { actions.setHomeLayout(id) }) {
                                Text(name)
                                if (id == homeState.homeLayout) Text("  •", color = LobbyColors.accent)
                            }
                        }
                    }
                }
                item { SectionHeader("App display") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppDisplayChoices.forEach { (id, name) ->
                            Button(onClick = { actions.setAppDisplay(id) }) {
                                Text(name)
                                if (id == homeState.appDisplay) Text("  •", color = LobbyColors.accent)
                            }
                        }
                    }
                }
            }
            SettingsCategory.Focus -> {
                item { SectionHeader("Focus style") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusChoices.forEach { (id, name) ->
                            Button(onClick = { actions.setFocusStyle(id) }) {
                                Text(name)
                                if (id == focusStyle) Text("  •", color = LobbyColors.accent)
                            }
                        }
                    }
                }
                item { SectionHeader("Material") }
                if (materialApplies(bgMode)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MaterialChoices.forEach { (id, name) ->
                                Button(onClick = { actions.setMaterial(id) }) {
                                    Text(name)
                                    if (id == material) Text("  •", color = LobbyColors.accent)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "Material only applies over a Photo background. Choose Photo under Background to use it.",
                            fontSize = 14.sp,
                            color = LobbyColors.mute,
                        )
                    }
                }
            }
            SettingsCategory.Typography -> items(FontChoices, key = { it.first }) { (id, name) ->
                FontLine(name, id, selected = id == homeFont) { actions.setHomeFont(id) }
            }
            SettingsCategory.Saver -> {
                item { ScreenSaverLine(screenSaverMinutes, actions) }
                item { Button(onClick = actions.openScreenSaverSettings) { Text("Choose the screen saver (Android settings)") } }
            }
            SettingsCategory.System -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (isDefaultHome) "Lobby is what the Home button opens." else "The Home button opens another launcher.",
                            fontSize = 16.sp, color = LobbyColors.soft,
                        )
                        Button(onClick = actions.openHomeSetup) { Text(if (isDefaultHome) "Home screen setup" else "Make Lobby the home screen") }
                        Button(onClick = actions.openSystemSettings) { Text("TV settings") }
                    }
                }
            }
            // Emby & Seerr: the connection plus everything the plugin keeps for this TV, grouped like the Samsung app.
            SettingsCategory.Media -> {
                item {
                    Column {
                        Text(tvName, fontSize = 18.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Connected to $host", fontSize = 13.sp, color = LobbyColors.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (data == null) {
                    item { Text(settings.error ?: "Loading settings…", fontSize = 15.sp, color = if (settings.error != null) LobbyColors.error else LobbyColors.soft) }
                } else {
                    item { SectionHeader("Home rows") }
                    items(data.rows, key = { "row-" + it.id }) { row ->
                        val index = data.rows.indexOfFirst { it.id == row.id }
                        RowSettingLine(row, canMoveUp = index > 0, canMoveDown = index < data.rows.lastIndex, actions)
                    }
                    item { SectionHeader("Continue Watching") }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Combine with Next Up", fontSize = 18.sp, color = LobbyColors.ink)
                                Text("Show Continue Watching and Next Up as a single Continue Watching row.", fontSize = 13.sp, color = LobbyColors.soft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Button(onClick = { actions.setCombineContinueNextUp(!homeState.combineContinueNextUp) }) {
                                Text(if (homeState.combineContinueNextUp) "Combined" else "Separate")
                            }
                        }
                    }
                    item { SectionHeader("Rows") }
                    item { NumberLine(NumberSetting.RowLimit, data, actions) }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Your teams", fontSize = 18.sp, color = LobbyColors.ink)
                                Text(teamSummary(data), fontSize = 13.sp, color = LobbyColors.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(onClick = actions.openTeams) { Text("Choose teams") }
                        }
                    }
                    item { NumberLine(NumberSetting.SportsLookahead, data, actions) }
                    item { SectionHeader("Playback") }
                    item { NumberLine(NumberSetting.PlayDelay, data, actions) }
                    item { NumberLine(NumberSetting.PlayTimeout, data, actions) }
                    item { SectionHeader("This TV") }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Server defaults", fontSize = 18.sp, color = LobbyColors.ink)
                                Text(if (data.customized) "This TV uses its own settings." else "This TV uses the server's defaults.", fontSize = 13.sp, color = LobbyColors.soft)
                            }
                            Button(onClick = actions.reset, enabled = data.customized) { Text("Use defaults") }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = actions.refreshRows) { Text("Refresh rows") }
                        Button(onClick = actions.disconnect) { Text("Disconnect") }
                    }
                }
            }
        }

        item { Box(Modifier.padding(top = 8.dp)) { Button(onClick = onBack) { Text("Back") } } }
      }
      if (showPreview) {
          SettingsPreview(
              homeState = homeState,
              address = address,
              bgMode = bgMode,
              wallpaper = wallpaper,
              homeFont = homeFont,
              modifier = Modifier
                  .weight(0.48f)
                  .fillMaxHeight()
                  .padding(end = 48.dp, top = 56.dp, bottom = 56.dp),
          )
      }
    }
}

/**
 * A live preview of Home shown beside each appearance menu, like the Samsung version's previewer. It lays the home
 * out responsively to fill the pane — hero backdrop art and theme-colour wash, hero text, left rail, a poster row
 * and the dock — filled with the user's real content, so it mirrors their home and reflects the current background,
 * typeface, focus style and material as they change settings.
 */
@Composable
private fun SettingsPreview(
    homeState: LobbyState,
    address: ServerAddress,
    bgMode: String,
    wallpaper: String,
    homeFont: String,
    modifier: Modifier = Modifier,
) {
    val font = fontFamilyFor(homeFont)
    val wp = remember(wallpaper) { Wallpapers.firstOrNull { it.id == wallpaper } ?: Wallpapers.first() }
    val shape = RoundedCornerShape(16.dp)
    val card12 = RoundedCornerShape(10.dp)
    val hero = homeState.home?.featured?.firstOrNull()
    val featuredCount = homeState.home?.featured?.size ?: 0
    val heading = hero?.tagline?.takeIf { it.isNotBlank() } ?: hero?.title?.takeIf { it.isNotBlank() } ?: "Featured Title"
    val body = hero?.overview?.takeIf { it.isNotBlank() }
        ?: "Your featured title's tagline and overview appear here, in your chosen typeface."
    val apps = remember(homeState.apps) { homeState.apps.distinctBy { it.packageName }.take(5) }
    val heroArt = address.imageUrl(hero?.backdropUrl ?: hero?.landscapeUrl, width = 800)
    val heroLogo = address.imageUrl(hero?.logoUrl, width = 300)
    val accent = rememberAccentColor(heroArt)
    // Watch = hero + rows + dock; Dock = no hero, open top; List = left app list.
    val isList = homeState.homeLayout == "list"
    val isDock = homeState.homeLayout == "dock"
    // Dock and List drop the hero, move the clock top-left and the rail to a horizontal strip top-right.
    val heroLess = isList || isDock
    val appDisplay = homeState.appDisplay
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(LobbyColors.background)
            .border(1.dp, Color(0x1FFFFFFF), shape),
    ) {
        if (bgMode == "photo") {
            AsyncImage(model = wp.fullUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0x4D000000)))
        }
        // Hero backdrop art, upper-right, dissolving toward the left and bottom (mirrors the real HeroBackdrop).
        // Dock drops the hero entirely, leaving an open top.
        if (!heroLess && heroArt != null) {
            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight(0.64f)
                    .align(Alignment.TopEnd)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.45f to Color.White), blendMode = BlendMode.DstIn)
                        drawRect(Brush.verticalGradient(0.55f to Color.White, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                    },
            ) {
                AsyncImage(model = heroArt, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        // Hero theme-colour wash, kept to the upper-left (mirrors the real home's accent glow).
        if (!heroLess && accent != Color.Transparent) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            0f to accent.copy(alpha = 0.30f),
                            0.55f to Color.Transparent,
                            center = Offset(size.width * 0.16f, size.height * 0.26f),
                            radius = size.width * 0.42f,
                        ),
                    )
                },
            )
        }
        // Hero text + the featured logo; in List the left column holds the app list, so the hero indents past it.
        // Dock has no hero.
        if (!heroLess) {
            Column(Modifier.fillMaxWidth(if (isList) 0.5f else 0.58f).align(Alignment.TopStart).padding(start = if (isList) 108.dp else 44.dp, end = 8.dp, top = 12.dp)) {
                Text("Good afternoon", fontFamily = font, fontSize = 8.sp, color = LobbyColors.soft)
                Spacer(Modifier.height(2.dp))
                Text(heading, fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(body, fontFamily = font, fontSize = 7.sp, color = LobbyColors.soft, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 9.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LobbyColors.focusFill)
                        .then(if (LobbyColors.focusStyle == "glass") Modifier.border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp)) else Modifier)
                        .padding(horizontal = 11.dp, vertical = 4.dp),
                ) {
                    Text("Play", fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 8.sp, color = LobbyColors.focusContent)
                }
            }
        }
        if (!heroLess && heroLogo != null) {
            AsyncImage(
                model = heroLogo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 30.dp).size(width = 82.dp, height = 36.dp),
            )
        }
        // Hero pager dots, one per featured item, mirroring the real home's bottom-right indicator.
        if (!heroLess && featuredCount > 1) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(end = 26.dp, top = 74.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(featuredCount.coerceAtMost(8)) { i ->
                    Box(
                        Modifier
                            .height(3.dp)
                            .width(if (i == 0) 12.dp else 3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i == 0) Color.White else Color(0x66FFFFFF)),
                    )
                }
            }
        }
        // Two real poster rows near the bottom; the second card of the first row carries the focus ring.
        // Dock and List free up the hero's band, so the previewer shows an extra row there.
        val rowCount = if (heroLess) 3 else 2
        val previewRows = remember(homeState.home, homeState.combineContinueNextUp, rowCount) {
            homeState.home?.rows
                ?.let { if (homeState.combineContinueNextUp) mergeContinueRows(it) else it }
                ?.filter { it.items.isNotEmpty() }
                ?.take(rowCount)
                .orEmpty()
        }
        Column(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth(if (isDock) 0.82f else 0.72f)
                .padding(start = if (isList) 108.dp else 24.dp, bottom = if (isList) 14.dp else 42.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val shown = if (previewRows.isNotEmpty()) previewRows else List(rowCount) { null }
            shown.forEachIndexed { ri, r ->
                val title = r?.title?.takeIf { it.isNotBlank() } ?: if (ri == 0) "Continue Watching" else "Next Up"
                // Title hugs its own row of cards; the outer spacing separates the two groups.
                Column {
                    Text(title, fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, lineHeight = 10.sp, color = LobbyColors.ink)
                    val rc = r?.items?.distinctBy { it.id }?.take(5).orEmpty()
                    // Always five equal slots so every card is the same size across both rows;
                    // rows with fewer items leave the trailing slots empty.
                    val count = if (rc.isNotEmpty()) rc.size else 5
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { i ->
                            if (i < count) {
                                val art = rc.getOrNull(i)?.let { address.imageUrl(it.landscapeUrl ?: it.posterUrl, width = 220) }
                                Box(
                                    Modifier.weight(1f).aspectRatio(1.72f).clip(card12).background(LobbyColors.card)
                                        .then(if (ri == 0 && i == 1) Modifier.border(1.dp, Color(0xCCFFFFFF), card12) else Modifier),
                                ) {
                                    if (art != null) AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        if (isList) {
            // List layout: a vertical app list down the left, centred vertically, following App display.
            Column(
                Modifier.align(Alignment.TopStart).fillMaxHeight().width(100.dp).padding(start = 12.dp, top = 40.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            ) {
                val slots = if (apps.isNotEmpty()) apps.size else 5
                repeat(slots.coerceAtMost(6)) { i ->
                    val bmp = apps.getOrNull(i)?.let { app -> app.icon?.toBitmap(240, 135)?.asImageBitmap() }
                    val name = apps.getOrNull(i)?.label ?: "App"
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                            .then(if (i == 0) Modifier.background(LobbyColors.focusPlate).border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(7.dp)) else Modifier)
                            .padding(horizontal = 5.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (appDisplay != "text") {
                            Box(Modifier.size(width = 24.dp, height = 16.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x2EFFFFFF)), contentAlignment = Alignment.Center) {
                                if (bmp != null) Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(2.dp))
                            }
                        }
                        if (appDisplay != "icons") {
                            Text(name, fontFamily = font, fontSize = 6.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        // The rail of nav icons. Watch keeps it vertical on the left; Dock and List use a horizontal strip top-right
        // with the date/time/weather moved to the top-left.
        if (heroLess) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(RailIcon.Search, RailIcon.Art, RailIcon.Apps, RailIcon.Settings).forEach { icon ->
                    Canvas(Modifier.size(12.dp)) { drawRailIcon(icon, Color(0xCCFFFFFF)) }
                }
            }
            Row(
                Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("9:41", fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = LobbyColors.ink)
                Text("72°", fontFamily = font, fontSize = 8.sp, color = LobbyColors.soft)
            }
        } else {
            Column(
                Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                listOf(RailIcon.Search, RailIcon.Art, RailIcon.Apps, RailIcon.Settings).forEach { icon ->
                    Canvas(Modifier.size(12.dp)) { drawRailIcon(icon, Color(0xCCFFFFFF)) }
                }
            }
        }
        // The dock, pinned bottom-centre (Dock and Watch; List shows the app list instead). The frosted veil follows
        // the Material, the focused tile shows the glass plate, and the tiles follow App display
        // (icon only / name only / both). Watch and Dock both keep the bottom dock; List does not.
        if (!isList) {
            // Over a photo the dock is a frosted bar whose veil follows the Material. On a dark background it stays
            // bare like the Samsung dock — no bar, no border — with a soft, light focus plate.
            val photo = bgMode == "photo"
            val dockShape = RoundedCornerShape(9.dp)
            val dockVeil = when (LobbyColors.material) {
                "frost" -> Color(0x59FFFFFF)
                "tint" -> Color(0x800A1016)
                else -> Color(0x730B0D10)
            }
            val tileW = 30.dp
            val iconH = 20.dp
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(dockShape)
                    .then(if (photo) Modifier.background(dockVeil).border(1.dp, Color(0x24FFFFFF), dockShape) else Modifier)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                val slots = if (apps.isNotEmpty()) apps.size else 5
                repeat(slots) { i ->
                    val bmp = apps.getOrNull(i)?.let { app -> app.icon?.toBitmap(240, 135)?.asImageBitmap() }
                    val name = apps.getOrNull(i)?.label ?: "App"
                    val focused = i == slots / 2
                    val tilePlate = when {
                        focused && photo -> Modifier.background(LobbyColors.focusPlate).border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(7.dp))
                        focused -> Modifier.background(Color(0x24FFFFFF)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(7.dp))
                        photo -> Modifier.background(Color(0x2EFFFFFF))
                        else -> Modifier
                    }
                    Column(
                        Modifier.width(tileW).clip(RoundedCornerShape(7.dp))
                            .then(tilePlate)
                            .padding(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (appDisplay != "text") {
                            Box(Modifier.fillMaxWidth().height(iconH).clip(RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                                if (bmp != null) Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            }
                        }
                        if (appDisplay != "icons") {
                            Text(name, fontFamily = font, fontSize = 5.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** A selectable wallpaper thumbnail with its name. */
@Composable
private fun WallpaperTile(wallpaper: Wallpaper, selected: Boolean, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        modifier = modifier,
        border = CardDefaults.border(
            border = if (selected) Border(BorderStroke(2.dp, LobbyColors.accent), shape = RoundedCornerShape(10.dp)) else Border.None,
        ),
    ) {
        Column {
            AsyncImage(
                model = wallpaper.thumbUri,
                contentDescription = wallpaper.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(96.dp),
            )
            Text(wallpaper.name, fontSize = 13.sp, color = LobbyColors.soft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        }
    }
}

/** A font choice, previewed in its own typeface, with a selected marker. */
@Composable
private fun FontLine(name: String, id: String, selected: Boolean, onSelect: () -> Unit) {
    Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
        Text(name, fontSize = 20.sp, fontFamily = fontFamilyFor(id), textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
        if (selected) Text("Selected", fontSize = 13.sp, color = LobbyColors.accent)
    }
}

/** A dock-app row: show/hide plus reorder. */
@Composable
private fun AppLine(
    label: String,
    hidden: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (hidden: Boolean) -> Unit,
    onMove: (delta: Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 18.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Button(onClick = { onToggle(!hidden) }, modifier = Modifier.width(120.dp)) {
            Text(if (hidden) "Hidden" else "Shown", textAlign = TextAlign.Center)
        }
        Button(onClick = { onMove(-1) }, enabled = canMoveUp) { Text("Up") }
        Button(onClick = { onMove(1) }, enabled = canMoveDown) { Text("Down") }
    }
}

/** A section header inside a settings detail list (Samsung's grouped headings). */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        color = LobbyColors.mute,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** A single show/hide row used by the Inputs category. */
@Composable
private fun ToggleLine(label: String, hidden: Boolean, onLabel: String = "Shown", offLabel: String = "Hidden", onSet: (hidden: Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 18.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Button(onClick = { onSet(!hidden) }, modifier = Modifier.width(140.dp)) {
            Text(if (hidden) offLabel else onLabel, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RowSettingLine(row: TvRowSetting, canMoveUp: Boolean, canMoveDown: Boolean, actions: SettingsActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(row.title, fontSize = 18.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!row.available && !row.reason.isNullOrBlank()) {
                Text(row.reason, fontSize = 13.sp, color = LobbyColors.soft)
            }
        }
        Button(onClick = { actions.toggleRow(row.id) }, modifier = Modifier.width(120.dp)) {
            Text(if (row.visible) "Shown" else "Hidden", textAlign = TextAlign.Center)
        }
        Button(onClick = { actions.moveRow(row.id, -1) }, enabled = canMoveUp) { Text("Up") }
        Button(onClick = { actions.moveRow(row.id, 1) }, enabled = canMoveDown) { Text("Down") }
    }
}

@Composable
private fun NumberLine(setting: NumberSetting, data: TvSettingsResponse, actions: SettingsActions) {
    val value = data.valueOf(setting)
    val range = data.ranges[setting.rangeKey]
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(setting.label, fontSize = 18.sp, color = LobbyColors.ink)
            val defaultText = range?.let { " Server default: ${it.default}${setting.unit}." }.orEmpty()
            Text(setting.hint + defaultText, fontSize = 13.sp, color = LobbyColors.soft)
        }
        Button(onClick = { actions.adjust(setting, -1) }, enabled = range == null || value > range.min) { Text("-") }
        Text("$value${setting.unit}", fontSize = 18.sp, color = LobbyColors.ink, textAlign = TextAlign.Center, modifier = Modifier.width(96.dp))
        Button(onClick = { actions.adjust(setting, 1) }, enabled = range == null || value < range.max) { Text("+") }
    }
}

/** Local to this device (not saved on the server): the idle delay before Lobby starts the system screen saver. */
@Composable
private fun ScreenSaverLine(minutes: Int, actions: SettingsActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Start automatically", fontSize = 18.sp, color = LobbyColors.ink)
            Text(
                "Starts your Android screen saver after Home sits idle. Android's own When to start setting still applies too.",
                fontSize = 13.sp, color = LobbyColors.soft,
            )
        }
        Button(onClick = { actions.stepScreenSaver(-1) }, enabled = minutes > SCREEN_SAVER_OPTIONS.first()) { Text("-") }
        Text(if (minutes <= 0) "Off" else "$minutes min", fontSize = 18.sp, color = LobbyColors.ink, textAlign = TextAlign.Center, modifier = Modifier.width(96.dp))
        Button(onClick = { actions.stepScreenSaver(1) }, enabled = minutes < SCREEN_SAVER_OPTIONS.last()) { Text("+") }
    }
}

private fun teamSummary(data: TvSettingsResponse): String = when (data.sportsTeams.size) {
    0 -> "No teams chosen yet"
    1 -> data.sportsTeams.first()
    else -> data.sportsTeams.take(3).joinToString(", ") + if (data.sportsTeams.size > 3) " and ${data.sportsTeams.size - 3} more" else ""
}

@Composable
private fun TeamsPicker(settings: SettingsState, actions: SettingsActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 56.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your teams", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = LobbyColors.ink)
                Text(
                    "Teams found in the next week of the Live TV guide. Pick the ones you follow.",
                    fontSize = 15.sp, color = LobbyColors.soft,
                )
                when {
                    settings.teamsLoading -> Text("Reading the guide…", fontSize = 15.sp, color = LobbyColors.soft)
                    settings.teamsMessage != null -> Text(settings.teamsMessage, fontSize = 15.sp, color = LobbyColors.error)
                }
            }
        }
        items(settings.teams.distinctBy { it.name }, key = { it.name }) { team ->
            TeamLine(team, actions)
        }
        item { Button(onClick = actions.back) { Text("Done") } }
    }
}

@Composable
private fun TeamLine(team: GuideTeam, actions: SettingsActions) {
    val detail = listOfNotNull(
        team.league,
        team.games.takeIf { it > 0 }?.let { if (it == 1) "1 game" else "$it games" },
    ).joinToString("  ·  ")
    Button(onClick = { actions.toggleTeam(team.name) }, modifier = Modifier.fillMaxWidth()) {
        Text(if (team.selected) "On" else "", modifier = Modifier.width(44.dp), fontSize = 16.sp, color = LobbyColors.accent)
        Text(team.name, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (detail.isNotEmpty()) Text(detail, fontSize = 14.sp, color = LobbyColors.soft)
    }
}
