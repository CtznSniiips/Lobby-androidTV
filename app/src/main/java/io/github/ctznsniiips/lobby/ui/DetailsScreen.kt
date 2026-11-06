@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.ctznsniiips.lobby.data.HomeItem
import io.github.ctznsniiips.lobby.data.ServerAddress
import io.github.ctznsniiips.lobby.data.TitleDetailsResponse
import io.github.ctznsniiips.lobby.data.TitlePerson

private val STATUS_NAMES = mapOf(
    "requested" to "Requested",
    "processing" to "Processing",
    "partial" to "Partly available",
    "available" to "Available",
)

private class Action(val label: String, val onClick: () -> Unit)

/**
 * A Seerr title: art, year/type/runtime/genres/rating, tagline, overview, credits, cast, and the actions
 * (Request, and Play/Resume when it is already in the library). Details come from Seerr through the plugin;
 * when they can't load, the screen still shows what the row itself carried and Request still works.
 */
@Composable
fun DetailsScreen(
    details: DetailsState,
    message: String?,
    address: ServerAddress,
    onRequest: () -> Unit,
    onPlay: () -> Unit,
    onClose: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val item = details.item
    val data = details.data

    val actions = buildList {
        if (!item.playItemId.isNullOrBlank() && item.playAction != null) {
            add(Action(if (item.playAction == "resume") "Resume" else "Play", onPlay))
        }
        if (item.requestStatus == null || item.requestStatus == "none" || item.requestStatus == "partial") {
            add(Action("Request", onRequest))
        }
        add(Action("Close", onClose))
    }
    val status = STATUS_NAMES[item.requestStatus]

    val focus = remember(item.tmdbId) { FocusRequester() }
    LaunchedEffect(item.tmdbId) { runCatching { focus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        address.imageUrl(item.backdropUrl ?: item.landscapeUrl, width = 1280)?.let { art ->
            AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, alpha = 0.35f, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(LobbyColors.background, LobbyColors.scrim, Color.Transparent)),
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { Header(item, data, details.loading, address) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    actions.forEachIndexed { index, action ->
                        Button(
                            onClick = action.onClick,
                            modifier = if (index == 0) Modifier.focusRequester(focus) else Modifier,
                        ) { Text(action.label) }
                    }
                    if (status != null) {
                        Text(status, fontSize = 16.sp, color = LobbyColors.accent)
                    }
                }
            }

            if (data != null && data.cast.isNotEmpty()) {
                item { CastRow(data.cast) }
            }
        }

        MessageToast(message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Header(item: HomeItem, data: TitleDetailsResponse?, loading: Boolean, address: ServerAddress) {
    val facts = buildList {
        item.year?.let { add(it.toString()) }
        add(if (item.mediaType == "tv") "TV show" else "Movie")
        data?.runtimeMinutes?.let { add(formatRuntime(it)) }
        data?.seasons?.let { add(if (it == 1) "1 season" else "$it seasons") }
        data?.genres?.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(", ")) }
        data?.rating?.let { add("★ $it") }
    }.joinToString("  ·  ")

    val overview = data?.overview?.takeIf { it.isNotBlank() } ?: item.overview
    val credits = buildList {
        data?.directors?.takeIf { it.isNotEmpty() }?.let { add("Directed by " + it.joinToString(", ")) }
        data?.writers?.takeIf { it.isNotEmpty() }?.let { add("Written by " + it.joinToString(", ")) }
        data?.creators?.takeIf { it.isNotEmpty() }?.let { add("Created by " + it.joinToString(", ")) }
        data?.networks?.takeIf { it.isNotEmpty() }?.let { add("On " + it.joinToString(", ")) }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        address.imageUrl(item.posterUrl ?: item.landscapeUrl, width = 400)?.let { poster ->
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(200.dp).height(300.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(item.title, fontSize = 36.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(facts, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            data?.tagline?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 18.sp, fontStyle = FontStyle.Italic)
            }
            if (!overview.isNullOrBlank()) {
                Text(overview, fontSize = 16.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            credits.forEach { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (loading) Text("Loading details…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CastRow(cast: List<TitlePerson>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cast", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(cast.distinctBy { it.name + it.character }, key = { it.name + it.character }) { person ->
                Column(Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (person.photoUrl != null) {
                        AsyncImage(
                            model = person.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(88.dp).clip(CircleShape),
                        )
                    } else {
                        Box(Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                            Text(initials(person.name), fontSize = 24.sp)
                        }
                    }
                    Text(person.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    person.character?.let {
                        Text(it, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun formatRuntime(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"

private fun initials(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
