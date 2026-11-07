package io.github.ctznsniiips.lobby.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/** Idle choices for the automatic start, in minutes; 0 is Off (the default, as on the Tizen app). */
val SCREEN_SAVER_OPTIONS = listOf(0, 2, 5, 10, 15, 30)

/**
 * Calls [onIdle] after [minutes] with no remote activity, to start the system screen saver. [enabled] should be
 * true only while Home is the visible screen. Leaving the app (to the Jellyfin/Emby app, say, or into the
 * screen saver itself) stops the countdown, and it restarts when Lobby is back in front.
 */
@Composable
fun IdleWatcher(enabled: Boolean, minutes: Int, activity: StateFlow<Int>, onIdle: () -> Unit) {
    val tick by activity.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(enabled, minutes, tick) {
        if (!enabled || minutes <= 0) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            delay(minutes * 60_000L)
            onIdle()
        }
    }
}
