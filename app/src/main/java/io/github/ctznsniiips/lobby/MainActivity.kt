package io.github.ctznsniiips.lobby

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctznsniiips.lobby.ui.AmbientScreen
import io.github.ctznsniiips.lobby.ui.ConnectScreen
import io.github.ctznsniiips.lobby.ui.DetailsScreen
import io.github.ctznsniiips.lobby.ui.HomeScreen
import io.github.ctznsniiips.lobby.ui.IdleWatcher
import io.github.ctznsniiips.lobby.ui.LobbyTheme
import io.github.ctznsniiips.lobby.ui.LobbyViewModel
import io.github.ctznsniiips.lobby.ui.RequesterDialog
import io.github.ctznsniiips.lobby.ui.SettingsActions
import io.github.ctznsniiips.lobby.ui.SettingsScreen
import io.github.ctznsniiips.lobby.ui.SearchScreen

class MainActivity : ComponentActivity() {
    private val viewModel: LobbyViewModel by viewModels()

    private val settingsActions by lazy {
        SettingsActions(
            toggleRow = viewModel::toggleRow,
            moveRow = viewModel::moveRow,
            adjust = viewModel::adjustNumber,
            openTeams = viewModel::openTeams,
            toggleTeam = viewModel::toggleTeam,
            reset = viewModel::resetSettings,
            stepScreenSaver = viewModel::stepScreenSaver,
            back = viewModel::closeSettings,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LobbyTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val paired = state.paired
                Box(Modifier.fillMaxSize()) {
                if (paired == null) {
                    ConnectScreen(
                        state = state.connect,
                        initialAddress = state.lastAddress,
                        onConnect = viewModel::connect,
                        onCancel = viewModel::cancelPairing,
                    )
                } else if (state.settings != null) {
                    BackHandler(onBack = viewModel::closeSettings)
                    SettingsScreen(
                        settings = state.settings!!,
                        screenSaverMinutes = state.screenSaverMinutes,
                        message = state.message,
                        actions = settingsActions,
                        onMessageShown = viewModel::clearMessage,
                    )
                } else if (state.details != null) {
                    BackHandler(onBack = viewModel::closeDetails)
                    DetailsScreen(
                        details = state.details!!,
                        message = state.message,
                        address = paired.address,
                        onRequest = viewModel::requestFromDetails,
                        onPlay = viewModel::playFromDetails,
                        onClose = viewModel::closeDetails,
                        onMessageShown = viewModel::clearMessage,
                    )
                } else if (state.search != null) {
                    BackHandler(onBack = viewModel::closeSearch)
                    SearchScreen(
                        search = state.search!!,
                        message = state.message,
                        address = paired.address,
                        onQuery = viewModel::onSearchQuery,
                        onItem = viewModel::onItem,
                        onMessageShown = viewModel::clearMessage,
                    )
                } else {
                    HomeScreen(
                        state = state,
                        address = paired.address,
                        onItem = viewModel::onItem,
                        onApp = viewModel::launchApp,
                        onSearch = viewModel::openSearch,
                        onAmbient = { viewModel.enterAmbient() },
                        onRefresh = viewModel::refreshHome,
                        onLobbySettings = viewModel::openSettings,
                        onSettings = viewModel::openSystemSettings,
                        onDisconnect = viewModel::disconnect,
                        onMessageShown = viewModel::clearMessage,
                    )
                }

                state.requestPicker?.let { picker ->
                    paired?.let { server ->
                        RequesterDialog(
                            picker = picker,
                            address = server.address,
                            onChoose = viewModel::chooseRequester,
                            onDismiss = viewModel::dismissRequesterPicker,
                        )
                    }
                }

                // Over everything else, so Home keeps its scroll position and focus underneath.
                state.ambient?.let { AmbientScreen(it) }

                val onHome = paired != null && state.settings == null && state.details == null && state.search == null &&
                    state.requestPicker == null && state.ambient == null
                IdleWatcher(
                    enabled = onHome,
                    minutes = state.screenSaverMinutes,
                    activity = viewModel.activity,
                    onIdle = { viewModel.enterAmbient(automatic = true) },
                )
                }
            }
        }
    }

    // A key that wakes ambient mode only wakes it; it doesn't also press something on Home.
    private var swallowKeyUp: Int? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (viewModel.state.value.ambient != null) {
                swallowKeyUp = event.keyCode
                viewModel.exitAmbient()
                return true
            }
            viewModel.userActivity()
        } else if (event.action == KeyEvent.ACTION_UP && swallowKeyUp == event.keyCode) {
            swallowKeyUp = null
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the Jellyfin/Emby app: Continue Watching and Next Up have probably changed.
        if (viewModel.state.value.paired != null) viewModel.refreshHome()
    }
}
