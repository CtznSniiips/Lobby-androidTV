@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.ctznsniiips.lobby.data.GuideTeam
import io.github.ctznsniiips.lobby.data.TvRowSetting
import io.github.ctznsniiips.lobby.data.TvSettingsResponse

/**
 * This TV's own settings, kept on the server by the plugin: which home rows show and in what order, how many
 * titles per row, the sports look-ahead, the playback hand-off timing, and the teams for Your Teams. Changes
 * apply on screen at once and are saved a moment later. Back returns to Home.
 */
@Composable
fun SettingsScreen(
    settings: SettingsState,
    screenSaverMinutes: Int,
    message: String?,
    actions: SettingsActions,
    onMessageShown: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        if (settings.teamsOpen) {
            TeamsPicker(settings, actions)
        } else {
            SettingsList(settings, screenSaverMinutes, actions)
        }
        MessageToast(message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SettingsList(settings: SettingsState, screenSaverMinutes: Int, actions: SettingsActions) {
    val data = settings.data
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Lobby settings", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                val sub: String = settings.error ?: when {
                    data == null -> "Loading…"
                    data.customized -> "This TV has its own settings."
                    else -> "Using the server's defaults until you change something."
                }
                Text(
                    sub,
                    fontSize = 15.sp,
                    color = if (settings.error != null) LobbyColors.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (data != null) {
            item { SectionTitle("Home rows") }
            items(data.rows, key = { it.id }) { row ->
                val index = data.rows.indexOfFirst { it.id == row.id }
                RowSettingLine(row, canMoveUp = index > 0, canMoveDown = index < data.rows.lastIndex, actions)
            }

            item { SectionTitle("Limits and timing") }
            items(NumberSetting.entries.toList(), key = { it.name }) { setting ->
                NumberLine(setting, data, actions)
            }

            item { SectionTitle("Screen saver") }
            item { ScreenSaverLine(screenSaverMinutes, actions) }
            item { Button(onClick = actions.openScreenSaverSettings) { Text("Choose the screen saver (Android settings)") } }

            item { SectionTitle("Sports") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = actions.openTeams) { Text("Choose teams") }
                    Text(teamSummary(data), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { Box(Modifier.padding(top = 12.dp)) { Button(onClick = actions.reset, enabled = data.customized) { Text("Use the server's defaults") } } }
        }

        item { Button(onClick = actions.back) { Text("Done") } }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun RowSettingLine(row: TvRowSetting, canMoveUp: Boolean, canMoveDown: Boolean, actions: SettingsActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(row.title, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!row.available && !row.reason.isNullOrBlank()) {
                Text(row.reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(setting.label, fontSize = 18.sp)
            val defaultText = range?.let { " Server default: ${it.default}${setting.unit}." }.orEmpty()
            Text(setting.hint + defaultText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { actions.adjust(setting, -1) }, enabled = range == null || value > range.min) { Text("-") }
        Text("$value${setting.unit}", fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.width(96.dp))
        Button(onClick = { actions.adjust(setting, 1) }, enabled = range == null || value < range.max) { Text("+") }
    }
}

/** Local to this device (not saved on the server): the idle delay before Lobby starts the system screen saver. */
@Composable
private fun ScreenSaverLine(minutes: Int, actions: SettingsActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Start automatically", fontSize = 18.sp)
            Text(
                "Starts your Android screen saver after Home sits idle. Android's own When to start setting still applies too.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = { actions.stepScreenSaver(-1) }, enabled = minutes > SCREEN_SAVER_OPTIONS.first()) { Text("-") }
        Text(if (minutes <= 0) "Off" else "$minutes min", fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.width(96.dp))
        Button(onClick = { actions.stepScreenSaver(1) }, enabled = minutes < SCREEN_SAVER_OPTIONS.last()) { Text("+") }
    }
}

private fun teamSummary(data: TvSettingsResponse): String = when (data.sportsTeams.size) {
    0 -> "No teams chosen. Your Teams stays empty until you pick some."
    1 -> data.sportsTeams.first()
    else -> data.sportsTeams.take(3).joinToString(", ") + if (data.sportsTeams.size > 3) " and ${data.sportsTeams.size - 3} more" else ""
}

@Composable
private fun TeamsPicker(settings: SettingsState, actions: SettingsActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your teams", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Teams found in the next week of the Live TV guide. Pick the ones you follow.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    settings.teamsLoading -> Text("Reading the guide…", fontSize = 15.sp)
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
        if (detail.isNotEmpty()) Text(detail, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
