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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.data.api.FeedMode
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.ui.FeedActionsHost
import wtf.mxl.pixmix.shared.ui.IllustFeed
import wtf.mxl.pixmix.shared.ui.IllustGrid
import wtf.mxl.pixmix.shared.ui.IllustTileFeed
import wtf.mxl.pixmix.shared.ui.WIDE_FEED_BREAKPOINT

@Composable
fun SearchScreen(component: SearchComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    val layout by component.layout.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = component::setQuery,
            placeholder = { Text("Search artworks, tags…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.error != null -> Text(
                        "Error: ${state.error}",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.items.isEmpty() && state.loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.items.isEmpty() -> Text(
                        "Type a tag and hit search",
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
