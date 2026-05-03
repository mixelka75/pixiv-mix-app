package wtf.mxl.pixmix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import org.koin.android.ext.android.inject
import wtf.mxl.pixmix.shared.RootContent
import wtf.mxl.pixmix.shared.auth.SessionStore
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.DiscoveryRepository
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.data.repository.RankingRepository
import wtf.mxl.pixmix.shared.data.repository.SearchRepository
import wtf.mxl.pixmix.shared.navigation.RootComponent
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.prefs.UserPrefs

class MainActivity : ComponentActivity() {

    private val sessionStore: SessionStore by inject()
    private val cookies: PersistentCookiesStorage by inject()
    private val discoveryRepository: DiscoveryRepository by inject()
    private val illustRepository: IllustRepository by inject()
    private val searchRepository: SearchRepository by inject()
    private val rankingRepository: RankingRepository by inject()
    private val userPrefs: UserPrefs by inject()
    private val likeStore: LocalLikeStore by inject()
    private val bookmarkStore: LocalBookmarkStore by inject()
    private val imageDownloader: ImageDownloader by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val root = RootComponent(
            componentContext = defaultComponentContext(),
            sessionStore = sessionStore,
            cookies = cookies,
            discoveryRepo = discoveryRepository,
            illustRepo = illustRepository,
            searchRepo = searchRepository,
            rankingRepo = rankingRepository,
            prefs = userPrefs,
            likeStore = likeStore,
            bookmarkStore = bookmarkStore,
            imageDownloader = imageDownloader,
        )

        setContent { RootContent(root) }
    }
}
