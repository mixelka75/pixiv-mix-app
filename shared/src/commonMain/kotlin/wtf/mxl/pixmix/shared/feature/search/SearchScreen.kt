package wtf.mxl.pixmix.shared.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.data.api.FeedMode
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.ui.ErrorState
import wtf.mxl.pixmix.shared.ui.FeedActionsHost
import wtf.mxl.pixmix.shared.ui.IllustFeed
import wtf.mxl.pixmix.shared.ui.IllustGrid
import wtf.mxl.pixmix.shared.ui.IllustGridSkeleton
import wtf.mxl.pixmix.shared.ui.IllustTileFeed
import wtf.mxl.pixmix.shared.ui.WIDE_FEED_BREAKPOINT
import wtf.mxl.pixmix.shared.util.userMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(component: SearchComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    val layout by component.layout.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Autofocus the search field when the user lands on the tab and there's no
    // existing query — saves them an extra tap on desktop especially.
    LaunchedEffect(Unit) {
        if (state.query.isBlank()) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = component::setQuery,
            placeholder = { Text("Поиск артов и тегов…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = component::clearQuery) {
                        Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { component.submit() }),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeedMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == state.mode,
                    onClick = { component.setMode(mode) },
                    label = { Text(mode.name) },
                )
            }
        }
        FeedActionsHost(controller = component.actions) { actions ->
            PullToRefreshBox(
                isRefreshing = state.loading && state.items.isNotEmpty(),
                onRefresh = component::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        state.error != null && state.items.isEmpty() -> ErrorState(
                            message = "Не удалось искать:\n${userMessageOf(state.error)}",
                            modifier = Modifier.align(Alignment.Center),
                            onRetry = component::refresh,
                        )
                        state.items.isEmpty() && state.loading -> IllustGridSkeleton()
                        state.items.isEmpty() && state.query.isBlank() -> Text(
                            "Введите тег или название",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        state.items.isEmpty() -> Text(
                            "Ничего не найдено",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        else -> when (layout) {
                            FeedLayout.Grid -> IllustGrid(
                                items = state.items,
                                onClick = component::openIllust,
                                onEndReached = component::loadMore,
                                actions = actions,
                            )
                            FeedLayout.Feed -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                if (maxWidth >= WIDE_FEED_BREAKPOINT) {
                                    IllustTileFeed(
                                        items = state.items,
                                        onClick = component::openIllust,
                                        onEndReached = component::loadMore,
                                        actions = actions,
                                    )
                                } else {
                                    IllustFeed(
                                        items = state.items,
                                        onClick = component::openIllust,
                                        onEndReached = component::loadMore,
                                        actions = actions,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun userMessageOf(error: String?): String {
    if (error.isNullOrBlank()) return "Неизвестная ошибка"
    return runCatching { Throwable(error).userMessage() }.getOrDefault(error)
}
