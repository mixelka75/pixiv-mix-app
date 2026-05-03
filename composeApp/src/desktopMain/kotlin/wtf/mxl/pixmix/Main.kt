package wtf.mxl.pixmix

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import wtf.mxl.pixmix.shared.RootContent
import wtf.mxl.pixmix.shared.auth.SessionStore
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.DiscoveryRepository
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.data.repository.RankingRepository
import wtf.mxl.pixmix.shared.data.repository.SearchRepository
import wtf.mxl.pixmix.shared.di.desktopPlatformModule
import wtf.mxl.pixmix.shared.di.sharedModule
import wtf.mxl.pixmix.shared.navigation.RootComponent
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.prefs.UserPrefs
import wtf.mxl.pixmix.shared.ui.buildImageLoader
import java.io.File

fun main() {
    val koin = startKoin {
        modules(desktopPlatformModule, sharedModule)
    }.koin

    val httpClient: HttpClient = koin.get()
    val cacheDir = File(System.getProperty("user.home"), ".cache/pixmix/images").apply { mkdirs() }
    SingletonImageLoader.setSafe { ctx: PlatformContext ->
        buildImageLoader(
            context = ctx,
            httpClient = httpClient,
            diskCachePath = cacheDir.absolutePath,
        )
    }

    application {
        // Decompose checks that ComponentContext is built on the platform main thread
        // (AWT EDT on JVM). Constructing it inside `application` puts us on that thread.
        val lifecycle = remember { LifecycleRegistry() }
        val rootComponent = remember {
            RootComponent(
                componentContext = DefaultComponentContext(lifecycle),
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
        }
        LaunchedEffect(Unit) { lifecycle.resume() }

        Window(
            onCloseRequest = ::exitApplication,
            title = "PixMix",
            icon = painterResource("icon.png"),
            state = rememberWindowState(width = 1200.dp, height = 900.dp),
        ) {
            RootContent(rootComponent)
        }
    }
}
