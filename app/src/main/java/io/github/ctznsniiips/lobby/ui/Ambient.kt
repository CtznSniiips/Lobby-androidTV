@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.ctznsniiips.lobby.data.HomeItem
import io.github.ctznsniiips.lobby.data.HomeResponse
import io.github.ctznsniiips.lobby.data.ServerAddress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Idle choices for the automatic start, in minutes; 0 is Off (the default, as on the Tizen app). */
val SCREEN_SAVER_OPTIONS = listOf(0, 2, 5, 10, 15, 30)

private const val SLIDE_MS = 15_000L
private const val CROSSFADE_MS = 1_500

data class AmbientSlide(val url: String, val title: String, val subtitle: String?)

data class AmbientState(val slides: List<AmbientSlide>)

/**
 * Artwork for ambient mode: the featured titles first (their large backdrops), then the rest of Home's landscape
 * art in random order. Live TV listings are skipped (their art is channel logos). Seerr art is upgraded to the
 * large TMDB size, as the Tizen app does for its featured area.
 */
fun buildAmbientSlides(home: HomeResponse, address: ServerAddress): List<AmbientSlide> {
    fun art(item: HomeItem, preferBackdrop: Boolean): String? {
        if (item.kind == "program") return null
        val raw = (if (preferBackdrop) item.backdropUrl ?: item.landscapeUrl else item.landscapeUrl ?: item.backdropUrl) ?: return null
        val large = raw.replace(Regex("^(https://image\\.tmdb\\.org/t/p/)w\\d+/"), "\$1w1280/")
        return address.imageUrl(large, width = 1920)
    }

    val featured = home.featured.mapNotNull { item ->
        art(item, preferBackdrop = true)?.let { AmbientSlide(it, item.title, item.tagline ?: item.meta) }
    }
    val rest = home.rows.flatMap { it.items }.mapNotNull { item ->
        art(item, preferBackdrop = false)?.let { AmbientSlide(it, item.title, item.subtitle) }
    }.shuffled()
    return (featured + rest).distinctBy { it.url }.take(40)
}

/**
 * Starts ambient mode after [minutes] with no remote activity. [enabled] should be true only while Home is the
 * visible screen. Leaving the app (to the Jellyfin/Emby app, say) stops the countdown, and it restarts on return.
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

/**
 * Slow full-screen slideshow with a faint title and a clock. Any button exits (MainActivity swallows that key
 * press so it doesn't also act on Home). The screen is kept awake while it shows.
 */
@Composable
fun AmbientScreen(ambient: AmbientState) {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val slides = ambient.slides
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(slides) {
        if (slides.size < 2) return@LaunchedEffect
        while (true) {
            delay(SLIDE_MS)
            index = (index + 1) % slides.size
        }
    }
    val clock by produceState(initialValue = timeText()) {
        while (true) {
            delay(15_000)
            value = timeText()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = index, animationSpec = tween(CROSSFADE_MS), label = "ambient") { i ->
            slides.getOrNull(i)?.let { Slide(it) }
        }

        val current = slides.getOrNull(index)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))),
        )
        if (current != null) {
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 56.dp, vertical = 44.dp)) {
                Text(current.title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                current.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 16.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        // The clock changes corner with each slide so no part of the screen carries a static element.
        Text(
            clock,
            fontSize = 34.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(if (index % 2 == 0) Alignment.BottomEnd else Alignment.TopEnd)
                .padding(horizontal = 56.dp, vertical = 44.dp),
        )
    }
}

@Composable
private fun Slide(slide: AmbientSlide) {
    // A slow push-in over the slide's lifetime (the Tizen Watch layout's Ken Burns drift).
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }
    val scale by animateFloatAsState(
        targetValue = if (grown) 1.1f else 1f,
        animationSpec = tween((SLIDE_MS + CROSSFADE_MS).toInt(), easing = LinearEasing),
        label = "kenburns",
    )
    AsyncImage(
        model = slide.url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

private fun timeText(): String = LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
