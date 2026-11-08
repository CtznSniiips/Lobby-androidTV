@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

/** Manrope, the same type family the Samsung app uses, bundled as static weights in res/font. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

object LobbyColors {
    val background = Color(0xFF0E1116)
    val accent = Color(0xFF6CB6FF)
    val error = Color(0xFFFF8A80)
    val scrim = Color(0xCC0E1116)

    /** Text tiers, bright enough to read over the dark background and backdrops. */
    val ink = Color(0xFFF4F2EE)
    val soft = Color(0xCCF4F2EE)
    val mute = Color(0x99F4F2EE)
}

@Composable
fun LobbyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = LobbyColors.background,
            surface = Color(0xFF181C24),
            onSurface = LobbyColors.ink,
            onSurfaceVariant = LobbyColors.soft,
            primary = LobbyColors.accent,
        ),
    ) {
        // Default every Text to Manrope and to light ink via LocalContentColor — not via the text style's own
        // colour, which would override the dark-on-light content colour a focused Button/Surface sets for itself
        // (that was the white-on-white bug).
        CompositionLocalProvider(
            LocalContentColor provides LobbyColors.ink,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Manrope),
            content = content,
        )
    }
}
