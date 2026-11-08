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
import androidx.compose.foundation.layout.fillMaxWidth
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

/** The settings sub-menus, matching the grouped layout of the Samsung app. */
private enum class SettingsCategory(val title: String) {
    Rows("Home rows"),
    Apps("Home apps"),
    Inputs("Inputs"),
    Limits("Limits & timing"),
    HomeApp("Home screen"),
    Saver("Screen saver"),
    Sports("Sports"),
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
            category == null -> CategoryList(settings, apps, inputs, hiddenApps, actions) { category = it }
            else -> CategoryDetail(category!!, settings, screenSaverMinutes, isDefaultHome, apps, inputs, hiddenApps, hiddenInputs, actions) { category = null }
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

        // Device-local categories don't need the server's settings.
        val shown = SettingsCategory.entries.filter { cat ->
            when (cat) {
                SettingsCategory.Apps -> apps.isNotEmpty()
                SettingsCategory.Inputs -> inputs.isNotEmpty()
                else -> data != null
            }
        }
        items(shown, key = { it.name }) { cat ->
            val summary = when (cat) {
                SettingsCategory.Apps -> "${apps.count { it.packageName !in hiddenApps }} of ${apps.size} apps shown"
                SettingsCategory.Inputs -> "${inputs.size} ${if (inputs.size == 1) "input" else "inputs"}"
                else -> data?.let { categorySummary(cat, it) }.orEmpty()
            }
            CategoryCard(cat.title, summary, onClick = { onOpen(cat) })
        }
        if (data != null) {
            item {
                Box(Modifier.padding(top = 12.dp)) {
                    Button(onClick = actions.reset, enabled = data.customized) { Text("Use the server's defaults") }
                }
            }
        }
        item { Button(onClick = actions.back) { Text("Done") } }
    }
}

@Composable
private fun CategoryCard(title: String, summary: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LobbyColors.ink)
                if (summary.isNotBlank()) Text(summary, fontSize = 14.sp, color = LobbyColors.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("›", fontSize = 26.sp, color = LobbyColors.mute)
        }
    }
}

private fun categorySummary(cat: SettingsCategory, data: TvSettingsResponse): String = when (cat) {
    SettingsCategory.Rows -> {
        val shown = data.rows.count { it.visible }
        "$shown of ${data.rows.size} rows shown"
    }
    SettingsCategory.Limits -> "${data.rowLimit} per row · ${data.sportsLookaheadHours} h look-ahead"
    SettingsCategory.HomeApp -> "Set whether the Home button opens Lobby"
    SettingsCategory.Saver -> "Screen saver and when it starts"
    SettingsCategory.Sports -> teamSummary(data)
    SettingsCategory.Apps, SettingsCategory.Inputs -> ""
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
                    SettingsCategory.Apps -> "Which apps appear in the dock at the bottom of Home."
                    SettingsCategory.Inputs -> "Which TV inputs appear on Home."
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
                ToggleLine(input.label, hidden = input.id in hiddenInputs) { hide -> actions.setInputHidden(input.id, hide) }
            }
            SettingsCategory.Rows -> data?.let {
                items(it.rows, key = { r -> r.id }) { row ->
                    val index = it.rows.indexOfFirst { r -> r.id == row.id }
                    RowSettingLine(row, canMoveUp = index > 0, canMoveDown = index < it.rows.lastIndex, actions)
                }
            }
            SettingsCategory.Limits -> data?.let {
                items(NumberSetting.entries.toList(), key = { s -> s.name }) { setting ->
                    NumberLine(setting, it, actions)
                }
            }
            SettingsCategory.HomeApp -> item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isDefaultHome) "Lobby is what the Home button opens." else "The Home button opens another launcher.",
                        fontSize = 16.sp, color = LobbyColors.soft,
                    )
                    Button(onClick = actions.openHomeSetup) { Text(if (isDefaultHome) "Home screen setup" else "Make Lobby the home screen") }
                }
            }
            SettingsCategory.Saver -> {
                item { ScreenSaverLine(screenSaverMinutes, actions) }
                item { Button(onClick = actions.openScreenSaverSettings) { Text("Choose the screen saver (Android settings)") } }
            }
            SettingsCategory.Sports -> item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(data?.let { teamSummary(it) }.orEmpty(), fontSize = 16.sp, color = LobbyColors.soft)
                    Button(onClick = actions.openTeams) { Text("Choose teams") }
                }
            }
        }

        item { Box(Modifier.padding(top = 8.dp)) { Button(onClick = onBack) { Text("Back") } } }
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

/** A single show/hide row used by the Inputs category. */
@Composable
private fun ToggleLine(label: String, hidden: Boolean, onSet: (hidden: Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 18.sp, color = LobbyColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Button(onClick = { onSet(!hidden) }, modifier = Modifier.width(120.dp)) {
            Text(if (hidden) "Hidden" else "Shown", textAlign = TextAlign.Center)
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
