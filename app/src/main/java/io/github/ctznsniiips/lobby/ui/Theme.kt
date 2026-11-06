@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object LobbyColors {
    val background = Color(0xFF0E1116)
    val accent = Color(0xFF6CB6FF)
    val error = Color(0xFFFF8A80)
    val scrim = Color(0xCC0E1116)
}

@Composable
fun LobbyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = LobbyColors.background,
            surface = Color(0xFF181C24),
            primary = LobbyColors.accent,
        ),
        content = content,
    )
}
