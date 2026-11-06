package io.github.ctznsniiips.lobby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctznsniiips.lobby.ui.ConnectScreen
import io.github.ctznsniiips.lobby.ui.DetailsScreen
import io.github.ctznsniiips.lobby.ui.HomeScreen
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
            back = viewModel::closeSettings,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LobbyTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val paired = state.paired
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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the Jellyfin/Emby app: Continue Watching and Next Up have probably changed.
        if (viewModel.state.value.paired != null) viewModel.refreshHome()
    }
}
