package wtf.mxl.pixmix.shared.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import wtf.mxl.pixmix.shared.feature.bookmarks.BookmarksComponent
import wtf.mxl.pixmix.shared.feature.home.HomeComponent
import wtf.mxl.pixmix.shared.feature.ranking.RankingComponent
import wtf.mxl.pixmix.shared.feature.search.SearchComponent
import wtf.mxl.pixmix.shared.feature.settings.SettingsComponent

enum class Tab { Home, Search, Bookmarks, Ranking, Settings }

class MainTabsComponent(
    componentContext: ComponentContext,
    homeFactory: (ComponentContext) -> HomeComponent,
    searchFactory: (ComponentContext) -> SearchComponent,
    rankingFactory: (ComponentContext) -> RankingComponent,
    bookmarksFactory: (ComponentContext) -> BookmarksComponent,
    settingsFactory: (ComponentContext) -> SettingsComponent,
) : ComponentContext by componentContext {

    val home: HomeComponent = homeFactory(childContext("home"))
    val search: SearchComponent = searchFactory(childContext("search"))
    val ranking: RankingComponent = rankingFactory(childContext("ranking"))
    val bookmarks: BookmarksComponent = bookmarksFactory(childContext("bookmarks"))
    val settings: SettingsComponent = settingsFactory(childContext("settings"))

    private val _selected = MutableStateFlow(Tab.Home)
    val selected: StateFlow<Tab> = _selected.asStateFlow()

    fun select(tab: Tab) { _selected.value = tab }
}
