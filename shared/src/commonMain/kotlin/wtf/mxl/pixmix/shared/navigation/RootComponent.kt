package wtf.mxl.pixmix.shared.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import wtf.mxl.pixmix.shared.auth.SessionStore
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.DiscoveryRepository
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.data.repository.RankingRepository
import wtf.mxl.pixmix.shared.data.repository.SearchRepository
import wtf.mxl.pixmix.shared.feature.bookmarks.BookmarksComponent
import wtf.mxl.pixmix.shared.feature.home.HomeComponent
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.feature.illust.IllustDetailComponent
import wtf.mxl.pixmix.shared.feature.illust.IllustViewerComponent
import wtf.mxl.pixmix.shared.feature.login.LoginComponent
import wtf.mxl.pixmix.shared.feature.main.MainTabsComponent
import wtf.mxl.pixmix.shared.feature.ranking.RankingComponent
import wtf.mxl.pixmix.shared.feature.search.SearchComponent
import wtf.mxl.pixmix.shared.feature.settings.SettingsComponent
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.prefs.UserPrefs

class RootComponent(
    componentContext: ComponentContext,
    private val sessionStore: SessionStore,
    private val cookies: PersistentCookiesStorage,
    private val discoveryRepo: DiscoveryRepository,
    private val illustRepo: IllustRepository,
    private val searchRepo: SearchRepository,
    private val rankingRepo: RankingRepository,
    private val prefs: UserPrefs,
    private val likeStore: LocalLikeStore,
    private val bookmarkStore: LocalBookmarkStore,
    private val imageDownloader: ImageDownloader,
) : ComponentContext by componentContext {

    private val nav = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> = childStack(
        source = nav,
        serializer = Config.serializer(),
        initialConfiguration = if (sessionStore.isLoggedIn()) Config.Main else Config.Login,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        sessionStore.session
            .drop(1)
            .onEach { session ->
                if (session != null) nav.replaceAll(Config.Main) else nav.replaceAll(Config.Login)
            }
            .launchIn(scope)

        lifecycle.doOnDestroy { scope.cancel() }
    }

    private fun openIllust(id: String) = nav.push(Config.IllustDetail(id))

    private fun createChild(config: Config, ctx: ComponentContext): Child = when (config) {
        Config.Login -> Child.Login(LoginComponent(ctx, sessionStore, cookies))

        Config.Main -> Child.Main(
            MainTabsComponent(
                componentContext = ctx,
                homeFactory = { c ->
                    HomeComponent(
                        componentContext = c,
                        discovery = discoveryRepo,
                        sessionStore = sessionStore,
                        cookies = cookies,
                        prefs = prefs,
                        likeStore = likeStore,
                        bookmarkStore = bookmarkStore,
                        illustRepo = illustRepo,
                        imageDownloader = imageDownloader,
                        onOpenIllust = ::openIllust,
                    )
                },
                searchFactory = { c ->
                    SearchComponent(
                        componentContext = c,
                        repo = searchRepo,
                        prefs = prefs,
                        likeStore = likeStore,
                        bookmarkStore = bookmarkStore,
                        illustRepo = illustRepo,
                        imageDownloader = imageDownloader,
                        onOpenIllust = ::openIllust,
                    )
                },
                rankingFactory = { c ->
                    RankingComponent(
                        componentContext = c,
                        repo = rankingRepo,
                        prefs = prefs,
                        likeStore = likeStore,
                        bookmarkStore = bookmarkStore,
                        illustRepo = illustRepo,
                        imageDownloader = imageDownloader,
                        onOpenIllust = ::openIllust,
                    )
                },
                bookmarksFactory = { c ->
                    BookmarksComponent(
                        componentContext = c,
                        store = bookmarkStore,
                        likeStore = likeStore,
                        illustRepo = illustRepo,
                        imageDownloader = imageDownloader,
                        prefs = prefs,
                        onOpenIllust = ::openIllust,
                    )
                },
                settingsFactory = { c ->
                    SettingsComponent(componentContext = c, prefs = prefs)
                },
            ),
        )

        is Config.IllustDetail -> Child.IllustDetail(
            IllustDetailComponent(
                componentContext = ctx,
                illustId = config.illustId,
                repo = illustRepo,
                prefs = prefs,
                likeStore = likeStore,
                bookmarkStore = bookmarkStore,
                imageDownloader = imageDownloader,
                onOpenViewer = { id, idx -> nav.push(Config.IllustViewer(id, idx)) },
                onOpenIllust = ::openIllust,
                onBack = { nav.pop() },
            ),
        )

        is Config.IllustViewer -> Child.IllustViewer(
            IllustViewerComponent(
                componentContext = ctx,
                illustId = config.illustId,
                initialIndex = config.startIndex,
                repo = illustRepo,
                onBack = { nav.pop() },
            ),
        )
    }

    sealed interface Child {
        data class Login(val component: LoginComponent) : Child
        data class Main(val component: MainTabsComponent) : Child
        data class IllustDetail(val component: IllustDetailComponent) : Child
        data class IllustViewer(val component: IllustViewerComponent) : Child
    }

    @Serializable
    sealed interface Config {
        @Serializable data object Login : Config
        @Serializable data object Main : Config
        @Serializable data class IllustDetail(val illustId: String) : Config
        @Serializable data class IllustViewer(val illustId: String, val startIndex: Int) : Config
    }
}
