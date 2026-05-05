package wtf.mxl.pixmix.shared.feature.ranking

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.ui.FeedActionsHost
import wtf.mxl.pixmix.shared.ui.IllustFeed
import wtf.mxl.pixmix.shared.ui.IllustGrid
import wtf.mxl.pixmix.shared.ui.IllustTileFeed
import wtf.mxl.pixmix.shared.ui.WIDE_FEED_BREAKPOINT

@Composable
fun RankingScreen(component: RankingComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    val layout by component.layout.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
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
                else -> FeedActionsHost(controller = component.actions) { actions ->
                    when (layout) {
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

private fun RankingPeriod.label(): String = when (this) {
    RankingPeriod.Daily -> "Daily"
    RankingPeriod.Weekly -> "Weekly"
    RankingPeriod.Monthly -> "Monthly"
    RankingPeriod.Rookie -> "Rookie"
    RankingPeriod.Original -> "Original"
    RankingPeriod.DailyR18 -> "Daily R-18"
    RankingPeriod.WeeklyR18 -> "Weekly R-18"
}
