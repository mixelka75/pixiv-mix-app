package wtf.mxl.pixmix.shared.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
fun HomeScreen(component: HomeComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    val layout by component.layout.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ModeBar(
                current = state.mode,
                onSelect = component::setMode,
                onRefresh = component::refresh,
                onLogout = component::logout,
            )
        },
    ) { padding ->
        FeedActionsHost(controller = component.actions) { actions ->
            PullToRefreshBox(
                isRefreshing = state.loading && state.items.isNotEmpty(),
                onRefresh = component::refresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        state.error != null && state.items.isEmpty() -> ErrorState(
                            message = "Не удалось загрузить ленту:\n${userMessageOf(state.error)}",
                            modifier = Modifier.align(Alignment.Center),
                            onRetry = component::refresh,
                        )
                        state.items.isEmpty() && state.loading -> IllustGridSkeleton()
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
                    if (state.loading && state.items.isNotEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        )
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

@Composable
private fun ModeBar(
    current: FeedMode,
    onSelect: (FeedMode) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "PixMix",
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FeedMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == current,
                    onClick = { onSelect(mode) },
                    label = { Text(mode.label()) },
                )
            }
            Box(modifier = Modifier.weight(1f, fill = true))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить ленту")
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
            }
        }
    }
}

private fun FeedMode.label(): String = when (this) {
    FeedMode.Safe -> "Safe"
    FeedMode.All -> "All"
    FeedMode.R18 -> "R-18"
}
