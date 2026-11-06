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
import io.github.ctznsniiips.lobby.data.SubmitRequestBody
import io.github.ctznsniiips.lobby.device.DockApp
import io.github.ctznsniiips.lobby.device.TvDevice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
)

class LobbyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessionStore(app)
    private val device = TvDevice(app)

    private val _state = MutableStateFlow(LobbyState(lastAddress = store.lastOrigin().orEmpty()))
    val state: StateFlow<LobbyState> = _state.asStateFlow()

    private var api: LobbyApi? = null
    private var pairingJob: Job? = null
    private var searchJob: Job? = null

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
        store.forget()
        api = null
        _state.update { it.copy(paired = null, home = null, connect = ConnectState(), message = null, loading = false, homeError = null, search = null, requestPicker = null) }
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
        val canPlay = !playId.isNullOrBlank() && (item.defaultAction != "request" || item.playAction != null)
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
            } else if (item.defaultAction == "request" && item.tmdbId != null && !item.mediaType.isNullOrBlank()) {
                request(client, item)
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
    }
}
