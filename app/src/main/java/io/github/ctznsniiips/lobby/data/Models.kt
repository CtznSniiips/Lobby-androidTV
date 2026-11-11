package io.github.ctznsniiips.lobby.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The Lobby plugins serialise in PascalCase, so every field is mapped explicitly.
// Field lists mirror TvController.cs / HomeBuilder.cs in CtznSniiips/Lobby.

// ---- Pairing ---------------------------------------------------------------------------------

@Serializable
data class StartPairingBody(
    @SerialName("DeviceName") val deviceName: String,
    /**
     * On Samsung this is the TV's DUID. The server matches it against Jellyfin/Emby session DeviceIds to
     * find the app to send playback to, so on Android TV we send an id the Jellyfin/Emby app is likely to
     * share (the Android ID). The admin can always override it when approving the pairing.
     */
    @SerialName("Duid") val duid: String? = null,
)

@Serializable
data class StartPairingResponse(
    @SerialName("PairingId") val pairingId: String = "",
    @SerialName("Code") val code: String = "",
    @SerialName("Secret") val secret: String = "",
    @SerialName("ExpiresInSeconds") val expiresInSeconds: Int = 900,
)

@Serializable
data class PairingStatusResponse(
    /** pending, approved, rejected or expired. */
    @SerialName("Status") val status: String = "expired",
    /** Only present once, on the first poll after approval. */
    @SerialName("Token") val token: String? = null,
    @SerialName("TvName") val tvName: String? = null,
)

@Serializable
data class PingResponse(
    @SerialName("TvName") val tvName: String? = null,
    @SerialName("UserName") val userName: String? = null,
)

// ---- Home / Search ---------------------------------------------------------------------------

@Serializable
data class HomeItem(
    @SerialName("Id") val id: String = "",
    /** movie, episode, program or discover. */
    @SerialName("Kind") val kind: String = "movie",
    @SerialName("Title") val title: String = "",
    @SerialName("Subtitle") val subtitle: String? = null,
    @SerialName("Year") val year: Int? = null,
    @SerialName("Overview") val overview: String? = null,
    /** Server-relative ("/Items/...") for library items, absolute TMDB URLs for Seerr items. */
    @SerialName("PosterUrl") val posterUrl: String? = null,
    @SerialName("LandscapeUrl") val landscapeUrl: String? = null,
    @SerialName("ProgressPercent") val progressPercent: Double? = null,
    @SerialName("PositionTicks") val positionTicks: Long? = null,
    @SerialName("RuntimeTicks") val runtimeTicks: Long? = null,
    /** Item id to send to /Lobby/Play (the channel id for Live TV programs). */
    @SerialName("PlayItemId") val playItemId: String? = null,
    /** resume, play or request. */
    @SerialName("DefaultAction") val defaultAction: String = "play",
    @SerialName("ChannelName") val channelName: String? = null,
    @SerialName("ChannelNumber") val channelNumber: String? = null,
    @SerialName("StartDate") val startDate: String? = null,
    @SerialName("EndDate") val endDate: String? = null,
    @SerialName("IsAiring") val isAiring: Boolean? = null,
    @SerialName("TmdbId") val tmdbId: Int? = null,
    /** movie or tv (Seerr items). */
    @SerialName("MediaType") val mediaType: String? = null,
    /** none, requested, processing, partial or available (Seerr items). */
    @SerialName("RequestStatus") val requestStatus: String? = null,
    /** For Seerr items already in the library: play or resume. */
    @SerialName("PlayAction") val playAction: String? = null,
    @SerialName("Tagline") val tagline: String? = null,
    @SerialName("BackdropUrl") val backdropUrl: String? = null,
    @SerialName("LogoUrl") val logoUrl: String? = null,
    @SerialName("Meta") val meta: String? = null,
    /** Featured items: game, resume, new or trending. */
    @SerialName("FeatureReason") val featureReason: String? = null,
)

@Serializable
data class HomeRow(
    @SerialName("Id") val id: String = "",
    @SerialName("Title") val title: String = "",
    /** landscape or poster. */
    @SerialName("Layout") val layout: String = "landscape",
    @SerialName("Items") val items: List<HomeItem> = emptyList(),
)

@Serializable
data class HomeResponse(
    @SerialName("TvName") val tvName: String? = null,
    @SerialName("UserName") val userName: String? = null,
    @SerialName("JellyfinUserId") val jellyfinUserId: String? = null,
    @SerialName("RequestsEnabled") val requestsEnabled: Boolean = false,
    @SerialName("GeneratedUtc") val generatedUtc: String? = null,
    @SerialName("Rows") val rows: List<HomeRow> = emptyList(),
    @SerialName("Featured") val featured: List<HomeItem> = emptyList(),
    @SerialName("Warnings") val warnings: List<String> = emptyList(),
    @SerialName("Weather") val weather: WeatherConfig? = null,
)

/** Where to show the weather, set by the admin in the plugin. The reading itself comes from Open-Meteo. */
@Serializable
data class WeatherConfig(
    @SerialName("Latitude") val latitude: Double? = null,
    @SerialName("Longitude") val longitude: Double? = null,
    @SerialName("Fahrenheit") val fahrenheit: Boolean = false,
) {
    val valid: Boolean get() = latitude != null && longitude != null
}

// ---- Play / Request --------------------------------------------------------------------------

@Serializable
data class PlayBody(
    @SerialName("ItemId") val itemId: String,
    /** play, resume or details. */
    @SerialName("Action") val action: String = "play",
    @SerialName("StartPositionTicks") val startPositionTicks: Long? = null,
)

@Serializable
data class DispatchResult(
    /** sent, queued or failed. */
    @SerialName("Status") val status: String = "failed",
    @SerialName("Message") val message: String? = null,
    @SerialName("SessionId") val sessionId: String? = null,
)

@Serializable
data class Requester(
    @SerialName("Id") val id: Int = 0,
    @SerialName("Name") val name: String = "",
    @SerialName("AvatarUrl") val avatarUrl: String? = null,
)

@Serializable
data class RequestersResponse(
    @SerialName("RequestsEnabled") val requestsEnabled: Boolean = false,
    @SerialName("Requesters") val requesters: List<Requester> = emptyList(),
    @SerialName("LastRequesterId") val lastRequesterId: Int? = null,
    @SerialName("UsingFallback") val usingFallback: Boolean = false,
)

@Serializable
data class SubmitRequestBody(
    @SerialName("TmdbId") val tmdbId: Int,
    /** movie or tv. */
    @SerialName("MediaType") val mediaType: String,
    @SerialName("SeerrUserId") val seerrUserId: Int = 0,
)

@Serializable
data class SubmitRequestResponse(
    /** requested, approved or failed. */
    @SerialName("Status") val status: String = "failed",
    @SerialName("Message") val message: String? = null,
    @SerialName("RequestId") val requestId: Int? = null,
)

// ---- Details (Seerr) ---------------------------------------------------------------------

@Serializable
data class TitlePerson(
    @SerialName("Name") val name: String = "",
    @SerialName("Character") val character: String? = null,
    /** Absolute TMDB image URL, when the person has a photo. */
    @SerialName("PhotoUrl") val photoUrl: String? = null,
)

@Serializable
data class TitleDetailsResponse(
    @SerialName("Tagline") val tagline: String? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("Genres") val genres: List<String> = emptyList(),
    /** Minutes: a movie's runtime, or a show's usual episode length. */
    @SerialName("RuntimeMinutes") val runtimeMinutes: Int? = null,
    @SerialName("Seasons") val seasons: Int? = null,
    @SerialName("Rating") val rating: Double? = null,
    @SerialName("Directors") val directors: List<String> = emptyList(),
    @SerialName("Writers") val writers: List<String> = emptyList(),
    @SerialName("Creators") val creators: List<String> = emptyList(),
    @SerialName("Networks") val networks: List<String> = emptyList(),
    @SerialName("Cast") val cast: List<TitlePerson> = emptyList(),
)

// ---- Per-TV settings -------------------------------------------------------------------------

@Serializable
data class TvRowSetting(
    @SerialName("Id") val id: String = "",
    @SerialName("Title") val title: String = "",
    /** library, livetv or seerr. */
    @SerialName("Source") val source: String = "library",
    @SerialName("Visible") val visible: Boolean = true,
    /** False when the row can't show anything yet (Seerr not set up, or no teams chosen). */
    @SerialName("Available") val available: Boolean = true,
    @SerialName("Reason") val reason: String? = null,
)

@Serializable
data class SettingRange(
    @SerialName("Min") val min: Int = 0,
    @SerialName("Max") val max: Int = Int.MAX_VALUE,
    @SerialName("Default") val default: Int = 0,
)

@Serializable
data class TvSettingsResponse(
    /** Every row, in this TV's display order. */
    @SerialName("Rows") val rows: List<TvRowSetting> = emptyList(),
    @SerialName("RowLimit") val rowLimit: Int = 20,
    @SerialName("SportsLookaheadHours") val sportsLookaheadHours: Int = 12,
    @SerialName("PlayDelayMs") val playDelayMs: Int = 0,
    @SerialName("PlayTimeoutSeconds") val playTimeoutSeconds: Int = 45,
    @SerialName("SportsTeams") val sportsTeams: List<String> = emptyList(),
    /** When true, Continue Watching and Next Up are shown as a single "Continue Watching" row. Default: separate. */
    @SerialName("CombineContinueNextUp") val combineContinueNextUp: Boolean = false,
    /** False while the TV is still on the server defaults. */
    @SerialName("Customized") val customized: Boolean = false,
    @SerialName("Ranges") val ranges: Map<String, SettingRange> = emptyMap(),
)

/** Only the fields that are set are sent; the server leaves the rest as they were (or following its defaults). */
@Serializable
data class SaveTvSettingsBody(
    @SerialName("RowOrder") val rowOrder: List<String>? = null,
    @SerialName("HiddenRows") val hiddenRows: List<String>? = null,
    @SerialName("RowLimit") val rowLimit: Int? = null,
    @SerialName("SportsLookaheadHours") val sportsLookaheadHours: Int? = null,
    @SerialName("PlayDelayMs") val playDelayMs: Int? = null,
    @SerialName("PlayTimeoutSeconds") val playTimeoutSeconds: Int? = null,
    @SerialName("SportsTeams") val sportsTeams: List<String>? = null,
    @SerialName("CombineContinueNextUp") val combineContinueNextUp: Boolean? = null,
)

@Serializable
data class GuideTeam(
    @SerialName("Name") val name: String = "",
    /** Best guess at the league, e.g. "NHL Hockey". May be missing. */
    @SerialName("League") val league: String? = null,
    /** Distinct games listed in the look-ahead window. */
    @SerialName("Games") val games: Int = 0,
    @SerialName("NextStartDate") val nextStartDate: String? = null,
    @SerialName("Selected") val selected: Boolean = false,
)

@Serializable
data class GuideTeamsResponse(
    @SerialName("Days") val days: Int = 7,
    @SerialName("Teams") val teams: List<GuideTeam> = emptyList(),
    /** Set when the guide couldn't be read (e.g. Live TV isn't set up). */
    @SerialName("Message") val message: String? = null,
)

// ---- Server probe ----------------------------------------------------------------------------

@Serializable
data class PublicSystemInfo(
    @SerialName("ServerName") val serverName: String? = null,
    @SerialName("Version") val version: String? = null,
)
