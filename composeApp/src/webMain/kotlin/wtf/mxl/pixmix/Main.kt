package wtf.mxl.pixmix

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.ktor.client.HttpClient
import kotlinx.browser.document
import org.koin.core.context.startKoin
import wtf.mxl.pixmix.shared.RootContent
import wtf.mxl.pixmix.shared.auth.SessionStore
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.DiscoveryRepository
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.data.repository.RankingRepository
import wtf.mxl.pixmix.shared.data.repository.SearchRepository
import wtf.mxl.pixmix.shared.di.sharedModule
import wtf.mxl.pixmix.shared.di.webPlatformModule
import wtf.mxl.pixmix.shared.navigation.RootComponent
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.prefs.UserPrefs
import wtf.mxl.pixmix.shared.ui.buildImageLoader

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val koin = startKoin {
        modules(webPlatformModule, sharedModule)
    }.koin

    val httpClient = koin.get<HttpClient>()
    SingletonImageLoader.setSafe { ctx: PlatformContext ->
        // No real disk filesystem on web — Coil's memory cache + the browser's own
        // HTTP cache do the heavy lifting.
        buildImageLoader(context = ctx, httpClient = httpClient)
    }

    val lifecycle = LifecycleRegistry()
    val rootComponent = RootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
        sessionStore = koin.get<SessionStore>(),
        cookies = koin.get<PersistentCookiesStorage>(),
        discoveryRepo = koin.get<DiscoveryRepository>(),
        illustRepo = koin.get<IllustRepository>(),
        searchRepo = koin.get<SearchRepository>(),
        rankingRepo = koin.get<RankingRepository>(),
        prefs = koin.get<UserPrefs>(),
        likeStore = koin.get<LocalLikeStore>(),
        bookmarkStore = koin.get<LocalBookmarkStore>(),
        imageDownloader = koin.get<ImageDownloader>(),
    )
    lifecycle.resume()

    ComposeViewport(document.body!!) {
        RootContent(rootComponent)
    }
}
