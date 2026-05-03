package wtf.mxl.pixmix.shared.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.feature.bookmarks.BookmarksScreen
import wtf.mxl.pixmix.shared.feature.home.HomeScreen
import wtf.mxl.pixmix.shared.feature.ranking.RankingScreen
import wtf.mxl.pixmix.shared.feature.search.SearchScreen
import wtf.mxl.pixmix.shared.feature.settings.SettingsScreen

private val WIDE_BREAKPOINT = 720.dp

@Composable
fun MainTabsScreen(component: MainTabsComponent, modifier: Modifier = Modifier) {
    val selected by component.selected.collectAsState()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_BREAKPOINT) {
            WideLayout(component = component, selected = selected)
        } else {
            CompactLayout(component = component, selected = selected)
        }
    }
}

@Composable
private fun CompactLayout(component: MainTabsComponent, selected: Tab) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { component.select(tab) },
                        icon = { Icon(tab.icon(), contentDescription = tab.label()) },
                        label = { Text(tab.label()) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabContent(component, selected)
        }
    }
}

@Composable
private fun WideLayout(component: MainTabsComponent, selected: Tab) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Spacer(Modifier.width(8.dp))
            Tab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = selected == tab,
                    onClick = { component.select(tab) },
                    icon = { Icon(tab.icon(), contentDescription = tab.label()) },
                    label = { Text(tab.label()) },
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            TabContent(component, selected)
        }
    }
}

@Composable
private fun TabContent(component: MainTabsComponent, selected: Tab) {
    when (selected) {
        Tab.Home -> HomeScreen(component.home)
        Tab.Search -> SearchScreen(component.search)
        Tab.Bookmarks -> BookmarksScreen(component.bookmarks)
        Tab.Ranking -> RankingScreen(component.ranking)
        Tab.Settings -> SettingsScreen(component.settings)
    }
}

private fun Tab.label(): String = when (this) {
    Tab.Home -> "Home"
    Tab.Search -> "Search"
    Tab.Bookmarks -> "Bookmarks"
    Tab.Ranking -> "Ranking"
    Tab.Settings -> "Settings"
}

private fun Tab.icon(): ImageVector = when (this) {
    Tab.Home -> Icons.Filled.Home
    Tab.Search -> Icons.Filled.Search
    Tab.Bookmarks -> Icons.Filled.Bookmark
    Tab.Ranking -> Icons.Filled.Leaderboard
    Tab.Settings -> Icons.Filled.Settings
}
