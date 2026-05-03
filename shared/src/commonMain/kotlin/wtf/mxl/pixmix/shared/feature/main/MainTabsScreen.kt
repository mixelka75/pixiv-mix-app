package wtf.mxl.pixmix.shared.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import wtf.mxl.pixmix.shared.feature.home.HomeScreen
import wtf.mxl.pixmix.shared.feature.ranking.RankingScreen
import wtf.mxl.pixmix.shared.feature.search.SearchScreen
import wtf.mxl.pixmix.shared.feature.settings.SettingsScreen

@Composable
fun MainTabsScreen(component: MainTabsComponent, modifier: Modifier = Modifier) {
    val selected by component.selected.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
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
            when (selected) {
                Tab.Home -> HomeScreen(component.home)
                Tab.Search -> SearchScreen(component.search)
                Tab.Ranking -> RankingScreen(component.ranking)
                Tab.Settings -> SettingsScreen(component.settings)
            }
        }
    }
}

private fun Tab.label(): String = when (this) {
    Tab.Home -> "Home"
    Tab.Search -> "Search"
    Tab.Ranking -> "Ranking"
    Tab.Settings -> "Settings"
}

private fun Tab.icon(): ImageVector = when (this) {
    Tab.Home -> Icons.Filled.Home
    Tab.Search -> Icons.Filled.Search
    Tab.Ranking -> Icons.Filled.Leaderboard
    Tab.Settings -> Icons.Filled.Settings
}
