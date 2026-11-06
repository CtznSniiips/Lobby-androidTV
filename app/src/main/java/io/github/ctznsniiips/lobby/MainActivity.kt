package io.github.ctznsniiips.lobby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctznsniiips.lobby.ui.ConnectScreen
import io.github.ctznsniiips.lobby.ui.HomeScreen
import io.github.ctznsniiips.lobby.ui.LobbyTheme
import io.github.ctznsniiips.lobby.ui.LobbyViewModel
import io.github.ctznsniiips.lobby.ui.SearchScreen

class MainActivity : ComponentActivity() {
    private val viewModel: LobbyViewModel by viewModels()

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
                        onSettings = viewModel::openSystemSettings,
                        onDisconnect = viewModel::disconnect,
                        onMessageShown = viewModel::clearMessage,
                    )
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
