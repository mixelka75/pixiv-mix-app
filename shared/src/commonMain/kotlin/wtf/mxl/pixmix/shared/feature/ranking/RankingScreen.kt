package wtf.mxl.pixmix.shared.feature.ranking

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
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
fun RankingScreen(component: RankingComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    val layout by component.layout.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Sticky chips: stay visible at any scroll position. Was a regular Row
            // inside the Column, scrolled away with the feed and forced a return-to-top
            // every time you wanted to switch period.
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RankingPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = p == state.period,
                            onClick = { component.setPeriod(p) },
                            label = { Text(p.label()) },
                        )
                    }
                }
            }
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
                            message = "Не удалось загрузить ранкинг:\n${userMessageOf(state.error)}",
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
                }
            }
        }
    }
}

private fun userMessageOf(error: String?): String {
    if (error.isNullOrBlank()) return "Неизвестная ошибка"
    return runCatching { Throwable(error).userMessage() }.getOrDefault(error)
}

private fun RankingPeriod.label(): String = when (this) {
    RankingPeriod.Daily -> "Daily"
    RankingPeriod.Weekly -> "Weekly"
    RankingPeriod.Monthly -> "Monthly"
    RankingPeriod.Rookie -> "Rookie"
    RankingPeriod.Original -> "Original"
    RankingPeriod.DailyR18 -> "Daily R-18"
    RankingPeriod.WeeklyR18 -> "Weekly R-18"
}
