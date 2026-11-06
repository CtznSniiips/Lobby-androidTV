@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.ctznsniiips.lobby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.ctznsniiips.lobby.data.HomeItem
import io.github.ctznsniiips.lobby.data.ServerAddress

/**
 * Search the library and Seerr in one box. The server answers with "In your library" and "Request from Seerr"
 * poster rows; picking a library title plays it, picking a Seerr title requests it, same as on Home.
 * Back returns to Home.
 */
@Composable
fun SearchScreen(
    search: SearchState,
    message: String?,
    address: ServerAddress,
    onQuery: (query: String, immediate: Boolean) -> Unit,
    onItem: (HomeItem) -> Unit,
    onMessageShown: () -> Unit,
) {
    var text by remember { mutableStateOf(search.query) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(LobbyColors.background)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Search", fontSize = 32.sp, fontWeight = FontWeight.Bold)

            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onQuery(it, false)
                },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 24.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onQuery(text, true)
                    // Drop from the keyboard into the first result.
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .border(2.dp, LobbyColors.accent, RoundedCornerShape(10.dp))
                    .padding(16.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text("Movies and shows", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    }
                },
            )

            val status = when {
                search.error != null -> search.error
                search.loading && search.rows.isEmpty() -> "Searching…"
                search.query.isBlank() -> "Type a title. Library matches play; others can be requested."
                search.searched && search.rows.isEmpty() -> "No results for “${search.query.trim()}”."
                else -> null
            }
            if (status != null) {
                Text(
                    status,
                    fontSize = 16.sp,
                    color = if (search.error != null) LobbyColors.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            search.warnings.forEach { warning ->
                Text(warning, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                items(search.rows.distinctBy { it.id }, key = { it.id }) { row -> MediaRow(row, address, onItem) }
            }
        }

        MessageToast(message, onMessageShown, Modifier.align(Alignment.BottomCenter))
    }
}
