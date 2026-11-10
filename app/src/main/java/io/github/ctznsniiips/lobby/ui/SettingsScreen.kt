@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
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
            category == null -> CategoryList(settings, apps, inputs, hiddenApps, homeFont, bgMode, focusStyle, material, actions) { category = it }
            else -> CategoryDetail(category!!, settings, screenSaverMinutes, isDefaultHome, apps, inputs, hiddenApps, hiddenInputs, tvName, host, homeFont, bgMode, wallpaper, focusStyle, material, actions) { category = null }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
