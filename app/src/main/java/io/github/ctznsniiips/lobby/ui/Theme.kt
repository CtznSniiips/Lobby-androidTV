@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import io.github.ctznsniiips.lobby.R

/** The three selectable type families (the Samsung app's names), bundled as static weights in res/font. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
)

val Newsreader = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold),
    Font(R.font.newsreader_bold, FontWeight.Bold),
)

/** The font ids offered in settings, with display names, in order. */
val FontChoices = listOf("manrope" to "Manrope", "geist" to "Geist", "newsreader" to "Newsreader")

fun fontFamilyFor(id: String): FontFamily = when (id) {
    "geist" -> Geist
    "newsreader" -> Newsreader
    else -> Manrope
}

/** A bundled wallpaper for Photo background mode. Images live in assets/wallpapers (Unsplash-licensed). */
data class Wallpaper(val id: String, val name: String) {
    val fullUri: String get() = "file:///android_asset/wallpapers/$id.jpg"
    val thumbUri: String get() = "file:///android_asset/wallpapers/$id-thumb.jpg"
}

/** The fifteen bundled wallpapers, matching the Samsung app's gallery. */
val Wallpapers = listOf(
    Wallpaper("misty", "Misty coast"),
    Wallpaper("cove", "Quiet cove"),
    Wallpaper("forest", "Forest mist"),
    Wallpaper("valley", "Mountain valley"),
    Wallpaper("night", "Desert stars"),
    Wallpaper("peaks", "Blue peaks"),
    Wallpaper("bridge", "Bridge at dusk"),
    Wallpaper("ridge", "Sunlit ridges"),
    Wallpaper("golden", "Golden hour"),
    Wallpaper("summit", "Sunlit summits"),
    Wallpaper("goldcoast", "Gold Coast dusk"),
    Wallpaper("sydney", "Sydney Harbour"),
    Wallpaper("amber", "Amber hills"),
    Wallpaper("mint", "Mint wave"),
    Wallpaper("alpine", "Alpine glow"),
)

/** The three Background modes, with display names. */
val BackgroundModes = listOf("photo" to "Photo", "dark" to "Dark", "light" to "Light")

/** A background/text colour set. The theme swaps this for the chosen Background mode (dark, light, photo). */
data class LobbyPalette(
    val background: Color,
    val surface: Color,
    /** The fill of settings cards/selectors (surfaceVariant). */
    val card: Color,
    val ink: Color,
    val soft: Color,
    val mute: Color,
    val scrim: Color,
    val light: Boolean,
)

val DarkPalette = LobbyPalette(
    background = Color(0xFF0E1116),
    surface = Color(0xFF181C24),
    card = Color(0xFF2A2E38),
    ink = Color(0xFFF4F2EE),
    soft = Color(0xCCF4F2EE),
    mute = Color(0x99F4F2EE),
    scrim = Color(0xCC0E1116),
    light = false,
)

val LightPalette = LobbyPalette(
    background = Color(0xFFF3F1EC),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFE3E0D8),
    ink = Color(0xFF16181C),
    soft = Color(0xC216181C),
    mute = Color(0x8C16181C),
    scrim = Color(0xBFF3F1EC),
    light = true,
)

/** Photo mode keeps light text — it sits over the wallpaper behind a scrim. */
val PhotoPalette = DarkPalette.copy(background = Color(0xFF0B0D10))

fun paletteFor(bgMode: String): LobbyPalette = when (bgMode) {
    "light" -> LightPalette
    "photo" -> PhotoPalette
    else -> DarkPalette
}

/**
 * The active colours. Backed by snapshot state the theme sets from the Background mode, so every `LobbyColors.ink`
 * read tracks the current palette and recomposes on a mode change. accent and error are fixed across modes.
 */
object LobbyColors {
    var palette by mutableStateOf(DarkPalette)
    val accent = Color(0xFF6CB6FF)
    val error = Color(0xFFFF8A80)
    val background: Color get() = palette.background
    val surface: Color get() = palette.surface
    val card: Color get() = palette.card
    val ink: Color get() = palette.ink
    val soft: Color get() = palette.soft
    val mute: Color get() = palette.mute
    val scrim: Color get() = palette.scrim
}

@Composable
fun LobbyTheme(font: String = "manrope", bgMode: String = "dark", content: @Composable () -> Unit) {
    LobbyColors.palette = paletteFor(bgMode)
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = LobbyColors.background,
            surface = LobbyColors.surface,
            onSurface = LobbyColors.ink,
            surfaceVariant = LobbyColors.card,
            onSurfaceVariant = LobbyColors.soft,
            primary = LobbyColors.accent,
        ),
    ) {
        // Default every Text to the chosen family and to light ink via LocalContentColor — not via the text style's
        // own colour, which would override the dark-on-light content colour a focused Button/Surface sets for itself.
        CompositionLocalProvider(
            LocalContentColor provides LobbyColors.ink,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = fontFamilyFor(font)),
            content = content,
        )
    }
}
