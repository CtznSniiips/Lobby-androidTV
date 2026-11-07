package io.github.ctznsniiips.lobby.ui

import io.github.ctznsniiips.lobby.data.GuideTeam
import io.github.ctznsniiips.lobby.data.TvSettingsResponse

/** The four numeric per-TV settings. [rangeKey] is the key in the server's "Ranges" map; [step] is one +/- press. */
enum class NumberSetting(
    val dirtyKey: String,
    val rangeKey: String,
    val label: String,
    val hint: String,
    val unit: String,
    val step: Int,
) {
    RowLimit("limit", "RowLimit", "Titles per row", "How many titles each home row shows.", "", 1),
    SportsLookahead("lookahead", "SportsLookaheadHours", "Sports look-ahead", "How far ahead Your Teams looks for games.", " h", 6),
    PlayDelay("delay", "PlayDelayMs", "Playback delay", "Wait after the Jellyfin/Emby app opens before sending play.", " ms", 250),
    PlayTimeout("timeout", "PlayTimeoutSeconds", "Playback timeout", "How long to keep trying if the app is slow to start.", " s", 5),
}

fun TvSettingsResponse.valueOf(setting: NumberSetting): Int = when (setting) {
    NumberSetting.RowLimit -> rowLimit
    NumberSetting.SportsLookahead -> sportsLookaheadHours
    NumberSetting.PlayDelay -> playDelayMs
    NumberSetting.PlayTimeout -> playTimeoutSeconds
}

fun TvSettingsResponse.withValue(setting: NumberSetting, value: Int): TvSettingsResponse = when (setting) {
    NumberSetting.RowLimit -> copy(rowLimit = value)
    NumberSetting.SportsLookahead -> copy(sportsLookaheadHours = value)
    NumberSetting.PlayDelay -> copy(playDelayMs = value)
    NumberSetting.PlayTimeout -> copy(playTimeoutSeconds = value)
}

/** Everything the settings screens render; the server's [TvSettingsResponse] plus the team picker. */
data class SettingsState(
    val loading: Boolean = true,
    val data: TvSettingsResponse? = null,
    val error: String? = null,
    val teamsOpen: Boolean = false,
    val teams: List<GuideTeam> = emptyList(),
    val teamsLoading: Boolean = false,
    val teamsMessage: String? = null,
)

/** Callbacks for the settings screens, so MainActivity passes one object instead of a dozen lambdas. */
class SettingsActions(
    val toggleRow: (String) -> Unit,
    val moveRow: (id: String, delta: Int) -> Unit,
    val adjust: (NumberSetting, direction: Int) -> Unit,
    val openTeams: () -> Unit,
    val toggleTeam: (String) -> Unit,
    val reset: () -> Unit,
    val stepScreenSaver: (direction: Int) -> Unit,
    val openScreenSaverSettings: () -> Unit,
    val back: () -> Unit,
)
