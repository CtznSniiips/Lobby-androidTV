package io.github.ctznsniiips.lobby.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.ctznsniiips.lobby.data.HomeItem
import io.github.ctznsniiips.lobby.data.HomeRow
import io.github.ctznsniiips.lobby.data.HomeResponse
import io.github.ctznsniiips.lobby.data.LobbyApi
import io.github.ctznsniiips.lobby.data.LobbyApiFactory
import io.github.ctznsniiips.lobby.data.PairedServer
import io.github.ctznsniiips.lobby.data.PlayBody
import io.github.ctznsniiips.lobby.data.Requester
import io.github.ctznsniiips.lobby.data.ServerProbe
import io.github.ctznsniiips.lobby.data.SessionStore
import io.github.ctznsniiips.lobby.data.StartPairingBody
import io.github.ctznsniiips.lobby.data.TitleDetailsResponse
import io.github.ctznsniiips.lobby.data.SaveTvSettingsBody
import io.github.ctznsniiips.lobby.data.SubmitRequestBody
import io.github.ctznsniiips.lobby.data.TvSettingsResponse
import io.github.ctznsniiips.lobby.device.DockApp
import io.github.ctznsniiips.lobby.device.TvDevice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException

data class ConnectState(
    val busy: Boolean = false,
    val error: String? = null,
    /** Shown once the server has issued a pairing code; the owner approves it in the plugin's admin page. */
    val pairingCode: String? = null,
    val serverName: String? = null,
)

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    /** search-library and search-seerr rows, as the server built them. */
    val rows: List<HomeRow> = emptyList(),
    val warnings: List<String> = emptyList(),
    val error: String? = null,
    /** True once a result (possibly empty) has come back for the current query. */
    val searched: Boolean = false,
)

/** Shown when this TV may request as more than one Seerr account (e.g. a couple sharing one TV). */
data class RequestPicker(
    val item: HomeItem,
    val requesters: List<Requester>,
    /** Whoever requested from this TV recently; focused first. */
    val lastRequesterId: Int?,
)

/** The Details screen for a Seerr title; [data] stays null if Seerr has nothing (the screen then shows what the row carried). */
data class DetailsState(
    val item: HomeItem,
    val loading: Boolean = true,
    val data: TitleDetailsResponse? = null,
)

data class LobbyState(
    val paired: PairedServer? = null,
    val connect: ConnectState = ConnectState(),
    val home: HomeResponse? = null,
    val loading: Boolean = false,
    val homeError: String? = null,
    val apps: List<DockApp> = emptyList(),
    /** One-line feedback after an action (play queued, requested, failed...). */
    val message: String? = null,
    val lastAddress: String = "",
    /** Non-null while the Search screen is open. */
    val search: SearchState? = null,
    /** Non-null while the "who's requesting?" dialog is open. */
    val requestPicker: RequestPicker? = null,
    /** Non-null while a title's Details screen is open (above Home or Search). */
    val details: DetailsState? = null,
    /** Non-null while the Lobby settings screens are open. */
    val settings: SettingsState? = null,
    /** Non-null while ambient mode is showing. */
    val ambient: AmbientState? = null,
    /** Idle minutes before ambient mode starts by itself; 0 is off. */
    val screenSaverMinutes: Int = 0,
)

class LobbyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessionStore(app)
    private val device = TvDevice(app)

    private val _state = MutableStateFlow(LobbyState(lastAddress = store.lastOrigin().orEmpty(), screenSaverMinutes = store.screenSaverMinutes()))
    val state: StateFlow<LobbyState> = _state.asStateFlow()

    // Bumped on every remote key press; the idle watcher restarts its countdown when it changes.
    private val _activity = MutableStateFlow(0)
    val activity: StateFlow<Int> = _activity.asStateFlow()

    private var api: LobbyApi? = null
    private var pairingJob: Job? = null
    private var searchJob: Job? = null
    private var detailsJob: Job? = null

    // Per-TV settings: edits show at once, then are saved after a pause, sending only the fields that changed.
    private val dirty = mutableSetOf<String>()
    private val saveMutex = Mutex()
    private var saveJob: Job? = null
    private var settingsChanged = false
    private val detailsCache = HashMap<String, TitleDetailsResponse>()

    init {
        store.load()?.let { onPaired(it, save = false) }
    }

    // ---- Pairing ------------------------------------------------------------------------------

    fun connect(addressInput: String) {
        pairingJob?.cancel()
        val origin = ServerProbe.normalise(addressInput)
        if (origin == null) {
            setConnect(ConnectState(error = "Enter the server's IP address (e.g. 192.168.1.10) or host name."))
            return
        }
        setConnect(ConnectState(busy = true))
        pairingJob = viewModelScope.launch {
            val probe = withContext(Dispatchers.IO) { ServerProbe.probe(origin) }
            val found = probe.getOrNull()
            if (found == null) {
                setConnect(ConnectState(error = probe.exceptionOrNull()?.message ?: "Couldn't reach that server."))
                return@launch
            }

            val pairingApi = LobbyApiFactory.create(found.address, token = null)
            val started = runCatching {
                pairingApi.startPairing(StartPairingBody(device.deviceName(), device.deviceId().ifBlank { null }))
            }.getOrElse {
                setConnect(ConnectState(error = describe(it, "The Lobby plugin didn't answer. Is it installed on that server?")))
                return@launch
            }

            setConnect(ConnectState(pairingCode = started.code, serverName = found.serverName))
            val deadline = System.currentTimeMillis() + started.expiresInSeconds * 1000L
            while (isActive && System.currentTimeMillis() < deadline) {
                delay(POLL_MS)
                val status = runCatching { pairingApi.pairingStatus(started.pairingId, started.secret) }.getOrNull() ?: continue
                when (status.status) {
                    "approved" -> {
                        val token = status.token
                        if (token.isNullOrBlank()) {
                            setConnect(ConnectState(error = "Approved, but the server sent no token. Start pairing again."))
                        } else {
                            onPaired(PairedServer(found.address, token, status.tvName.orEmpty()), save = true)
                        }
                        return@launch
                    }
                    "rejected" -> { setConnect(ConnectState(error = "Pairing was turned down on the server.")); return@launch }
                    "expired" -> { setConnect(ConnectState(error = "The pairing code expired. Try again.")); return@launch }
                }
            }
            setConnect(ConnectState(error = "The pairing code expired. Try again."))
        }
    }

    fun cancelPairing() {
        pairingJob?.cancel()
        setConnect(ConnectState())
    }

    fun disconnect() {
        pairingJob?.cancel()
        searchJob?.cancel()
        detailsJob?.cancel()
        saveJob?.cancel()
        dirty.clear()
        store.forget()
        api = null
        _state.update { it.copy(paired = null, home = null, connect = ConnectState(), message = null, loading = false, homeError = null, search = null, requestPicker = null, details = null, settings = null, ambient = null) }
    }

    private fun onPaired(server: PairedServer, save: Boolean) {
        if (save) store.save(server)
        api = LobbyApiFactory.create(server.address, server.token)
        _state.update { it.copy(paired = server, connect = ConnectState(), lastAddress = server.address.origin, apps = device.dockApps()) }
        refreshHome()
    }

    // ---- Home ---------------------------------------------------------------------------------

    fun refreshHome() {
        val client = api ?: return
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, homeError = null, apps = device.dockApps()) }
        viewModelScope.launch {
            runCatching { client.home() }
                .onSuccess { home -> _state.update { it.copy(home = home, loading = false) } }
                .onFailure { e ->
                    if (e is HttpException && e.code() == 401) {
                        // The server no longer knows this TV: drop the token and pair again.
                        disconnect()
                        setConnect(ConnectState(error = "This TV isn't paired any more. Connect it again."))
                    } else {
                        _state.update { it.copy(loading = false, homeError = describe(e, "Couldn't load Home.")) }
                    }
                }
        }
    }

    // ---- Actions ------------------------------------------------------------------------------

    fun onItem(item: HomeItem) {
        val client = api ?: return
        val playId = item.playItemId
        // Partly available shows open Details instead, which offers both Play and Request (for the missing seasons).
        val canPlay = !playId.isNullOrBlank() && (item.defaultAction != "request" || item.playAction != null) && item.requestStatus != "partial"
        viewModelScope.launch {
            if (canPlay) {
                val action = item.playAction ?: if (item.defaultAction == "resume") "resume" else "play"
                val result = runCatching { client.play(PlayBody(itemId = playId!!, action = action)) }
                    .getOrElse { say(describe(it, "Couldn't start playback.")); return@launch }
                if (result.status == "failed") {
                    say(result.message ?: "The server couldn't start playback.")
                } else if (!device.launchPlaybackApp()) {
                    say("Install the Jellyfin or Emby app on this device so Lobby can hand playback to it.")
                }
            } else if (item.tmdbId != null && !item.mediaType.isNullOrBlank()) {
                // Seerr titles open Details first, like the request screen on the Tizen app.
                openDetails(item)
            }
        }
    }

    private suspend fun request(client: LobbyApi, item: HomeItem) {
        val who = runCatching { client.requesters() }.getOrElse { say(describe(it, "Couldn't load requesters.")); return }
        if (!who.requestsEnabled) { say("Requests are turned off on this server."); return }
        when (who.requesters.size) {
            0 -> say("This TV's user isn't linked to a Seerr account.")
            // One allowed account (or the server's fallback account): nothing to choose.
            1 -> submitRequest(client, item, who.requesters.first().id)
            else -> _state.update { it.copy(requestPicker = RequestPicker(item, who.requesters, who.lastRequesterId)) }
        }
    }

    private suspend fun submitRequest(client: LobbyApi, item: HomeItem, seerrUserId: Int) {
        val result = runCatching { client.request(SubmitRequestBody(item.tmdbId!!, item.mediaType!!, seerrUserId)) }
            .getOrElse { say(describe(it, "Couldn't send the request.")); return }
        say(result.message ?: if (result.status == "failed") "Request failed." else "Requested.")
        if (result.status != "failed") {
            val newStatus = if (result.status == "approved") "processing" else "requested"
            _state.update { s ->
                val d = s.details
                if (d == null || d.item.tmdbId != item.tmdbId) s
                else s.copy(details = d.copy(item = d.item.copy(requestStatus = newStatus)))
            }
            refreshHome()
            // Searching: show the new "requested" status on the card.
            _state.value.search?.query?.takeIf { it.isNotBlank() }?.let { runSearch(it) }
        }
    }

    fun chooseRequester(seerrUserId: Int) {
        val picker = _state.value.requestPicker ?: return
        val client = api ?: return
        _state.update { it.copy(requestPicker = null) }
        viewModelScope.launch { submitRequest(client, picker.item, seerrUserId) }
    }

    fun dismissRequesterPicker() = _state.update { it.copy(requestPicker = null) }

    // ---- Ambient mode -------------------------------------------------------------------------

    fun userActivity() = _activity.update { it + 1 }

    /** Starts the slideshow from the art already on Home. [automatic] (the idle timer) stays quiet if there is none. */
    fun enterAmbient(automatic: Boolean = false) {
        val s = _state.value
        val home = s.home
        val address = s.paired?.address
        val slides = if (home != null && address != null) buildAmbientSlides(home, address) else emptyList()
        if (slides.isEmpty()) {
            if (!automatic) say("No artwork on Home to show yet. Let Home finish loading first.")
            return
        }
        _state.update { it.copy(ambient = AmbientState(slides)) }
    }

    fun exitAmbient() = _state.update { it.copy(ambient = null) }

    /** Steps the automatic-start delay through [SCREEN_SAVER_OPTIONS] (Off, 2, 5, 10, 15, 30 minutes). */
    fun stepScreenSaver(direction: Int) {
        val current = SCREEN_SAVER_OPTIONS.indexOf(_state.value.screenSaverMinutes).coerceAtLeast(0)
        val next = SCREEN_SAVER_OPTIONS[(current + direction).coerceIn(0, SCREEN_SAVER_OPTIONS.lastIndex)]
        store.setScreenSaverMinutes(next)
        _state.update { it.copy(screenSaverMinutes = next) }
    }

    // ---- Lobby settings (this TV's own rows, limits and teams) ---------------------------------

    fun openSettings() {
        val client = api ?: return
        dirty.clear()
        settingsChanged = false
        _state.update { it.copy(settings = SettingsState()) }
        viewModelScope.launch {
            runCatching { client.settings() }
                .onSuccess { data -> updateSettings { it.copy(loading = false, data = data) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    updateSettings { it.copy(loading = false, error = describe(e, "Couldn't load settings.")) }
                }
        }
    }

    /** Back: closes the team picker first, then the settings; pending edits are saved straight away. */
    fun closeSettings() {
        val current = _state.value.settings ?: return
        if (current.teamsOpen) {
            updateSettings { it.copy(teamsOpen = false) }
            return
        }
        saveJob?.cancel()
        val client = api
        _state.update { it.copy(settings = null) }
        viewModelScope.launch {
            if (client != null) withContext(NonCancellable) { performSave(client, current.data) }
            // Row order, visibility and limits change what Home shows.
            if (settingsChanged) refreshHome()
        }
    }

    fun toggleRow(id: String) = editSettings("rows") { d ->
        d.copy(rows = d.rows.map { if (it.id == id) it.copy(visible = !it.visible) else it })
    }

    fun moveRow(id: String, delta: Int) = editSettings("rows") { d ->
        val rows = d.rows.toMutableList()
        val from = rows.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in rows.indices) d else d.copy(rows = rows.apply { add(to, removeAt(from)) })
    }

    fun adjustNumber(setting: NumberSetting, direction: Int) = editSettings(setting.dirtyKey) { d ->
        val range = d.ranges[setting.rangeKey]
        val next = (d.valueOf(setting) + direction * setting.step).coerceIn(range?.min ?: 0, range?.max ?: Int.MAX_VALUE)
        d.withValue(setting, next)
    }

    fun toggleTeam(name: String) {
        editSettings("teams") { d ->
            val chosen = d.sportsTeams.any { it.equals(name, ignoreCase = true) }
            d.copy(sportsTeams = if (chosen) d.sportsTeams.filterNot { it.equals(name, ignoreCase = true) } else d.sportsTeams + name)
        }
        updateSettings { st -> st.copy(teams = st.teams.map { if (it.name == name) it.copy(selected = !it.selected) else it }) }
    }

    fun openTeams() {
        val client = api ?: return
        updateSettings { it.copy(teamsOpen = true, teamsLoading = true, teamsMessage = null) }
        viewModelScope.launch {
            runCatching { client.sportsTeams(TEAM_GUIDE_DAYS) }
                .onSuccess { r -> updateSettings { it.copy(teams = r.teams, teamsMessage = r.message, teamsLoading = false) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    updateSettings { it.copy(teamsLoading = false, teamsMessage = describe(e, "Couldn't load teams.")) }
                }
        }
    }

    /** Back to the server's defaults for this TV. */
    fun resetSettings() {
        val client = api ?: return
        saveJob?.cancel()
        dirty.clear()
        viewModelScope.launch {
            runCatching { client.resetSettings() }
                .onSuccess { data ->
                    settingsChanged = true
                    updateSettings { it.copy(data = data) }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    say(describe(e, "Couldn't reset settings."))
                }
        }
    }

    private fun updateSettings(change: (SettingsState) -> SettingsState) {
        _state.update { s -> s.settings?.let { s.copy(settings = change(it)) } ?: s }
    }

    private fun editSettings(field: String, change: (TvSettingsResponse) -> TvSettingsResponse) {
        val client = api ?: return
        if (_state.value.settings?.data == null) return
        updateSettings { st -> st.data?.let { st.copy(data = change(it).copy(customized = true)) } ?: st }
        dirty.add(field)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SETTINGS_SAVE_MS)
            // Once the request is out it finishes even if a newer edit cancels this job.
            withContext(NonCancellable) { performSave(client, _state.value.settings?.data) }
        }
    }

    private suspend fun performSave(client: LobbyApi, data: TvSettingsResponse?) {
        saveMutex.withLock {
            if (data == null || dirty.isEmpty()) return@withLock
            val fields = dirty.toSet()
            dirty.clear()
            val body = SaveTvSettingsBody(
                rowOrder = if ("rows" in fields) data.rows.map { it.id } else null,
                hiddenRows = if ("rows" in fields) data.rows.filter { !it.visible }.map { it.id } else null,
                rowLimit = if ("limit" in fields) data.rowLimit else null,
                sportsLookaheadHours = if ("lookahead" in fields) data.sportsLookaheadHours else null,
                playDelayMs = if ("delay" in fields) data.playDelayMs else null,
                playTimeoutSeconds = if ("timeout" in fields) data.playTimeoutSeconds else null,
                sportsTeams = if ("teams" in fields) data.sportsTeams else null,
            )
            runCatching { client.saveSettings(body) }
                .onSuccess { saved ->
                    settingsChanged = true
                    // Take the server's clamped values, unless the user has already changed something else since.
                    if (dirty.isEmpty()) updateSettings { it.copy(data = saved) }
                }
                .onFailure { e ->
                    dirty.addAll(fields)
                    say(describe(e, "Couldn't save settings."))
                }
        }
    }

    // ---- Details ------------------------------------------------------------------------------

    fun openDetails(item: HomeItem) {
        val client = api ?: return
        val mediaType = item.mediaType ?: return
        val tmdb = item.tmdbId ?: return
        val key = "$mediaType:$tmdb"
        val cached = detailsCache[key]
        detailsJob?.cancel()
        _state.update { it.copy(details = DetailsState(item, loading = cached == null, data = cached)) }
        if (cached != null) return
        detailsJob = viewModelScope.launch {
            val data = runCatching { client.details(mediaType, tmdb) }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
            if (data != null) detailsCache[key] = data
            _state.update { s ->
                val d = s.details
                if (d == null || d.item.tmdbId != tmdb) s else s.copy(details = d.copy(loading = false, data = data))
            }
        }
    }

    fun closeDetails() {
        detailsJob?.cancel()
        _state.update { it.copy(details = null) }
    }

    /** The Request button on the Details screen; asks who is requesting when the TV has several accounts. */
    fun requestFromDetails() {
        val client = api ?: return
        val item = _state.value.details?.item ?: return
        viewModelScope.launch { request(client, item) }
    }

    /** Play/Resume from Details, for a title that is also in the library. */
    fun playFromDetails() {
        val item = _state.value.details?.item ?: return
        closeDetails()
        onItem(item)
    }

    // ---- Search -------------------------------------------------------------------------------

    fun openSearch() = _state.update { it.copy(search = SearchState()) }

    fun closeSearch() {
        searchJob?.cancel()
        _state.update { it.copy(search = null) }
    }

    /** Typing searches after a short pause (the server also queries Seerr/TMDB); [immediate] skips the pause. */
    fun onSearchQuery(query: String, immediate: Boolean = false) {
        searchJob?.cancel()
        _state.update { s -> s.copy(search = (s.search ?: SearchState()).copy(query = query)) }
        if (query.isBlank()) {
            _state.update { s ->
                s.copy(search = s.search?.copy(rows = emptyList(), warnings = emptyList(), loading = false, searched = false, error = null))
            }
            return
        }
        searchJob = viewModelScope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            runSearch(query)
        }
    }

    private suspend fun runSearch(query: String) {
        val client = api ?: return
        val term = query.trim()
        _state.update { s -> s.copy(search = s.search?.copy(loading = true, error = null)) }
        runCatching { client.search(term) }
            .onSuccess { result ->
                _state.update { s ->
                    // Ignore an answer for a query the user has already typed past.
                    if (s.search == null || s.search.query.trim() != term) s
                    else s.copy(
                        search = s.search.copy(
                            rows = result.rows.filter { it.items.isNotEmpty() },
                            warnings = result.warnings,
                            loading = false,
                            searched = true,
                        ),
                    )
                }
            }
            .onFailure { e ->
                if (e is CancellationException) throw e
                _state.update { s -> s.copy(search = s.search?.copy(loading = false, error = describe(e, "Search failed."))) }
            }
    }

    fun launchApp(app: DockApp) {
        if (!device.launchApp(app.packageName)) say("Couldn't open ${app.label}.")
    }

    fun openSystemSettings() {
        if (!device.openSystemSettings()) say("Couldn't open Settings.")
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    // ---- Helpers ------------------------------------------------------------------------------

    private fun say(text: String) = _state.update { it.copy(message = text) }

    private fun setConnect(c: ConnectState) = _state.update { it.copy(connect = c) }

    private fun describe(e: Throwable, fallback: String): String = when {
        e is HttpException && e.code() == 401 -> "This TV isn't paired any more."
        e is HttpException && e.code() == 404 -> "The server answered, but the Lobby plugin isn't installed there."
        e is HttpException -> "$fallback (HTTP ${e.code()})"
        else -> fallback
    }

    private companion object {
        const val POLL_MS = 3000L
        const val SEARCH_DEBOUNCE_MS = 500L
        const val SETTINGS_SAVE_MS = 600L
        const val TEAM_GUIDE_DAYS = 7
    }
}
