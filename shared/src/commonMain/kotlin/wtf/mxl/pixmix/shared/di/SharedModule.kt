package wtf.mxl.pixmix.shared.di

import org.koin.dsl.module
import wtf.mxl.pixmix.shared.auth.SessionStore
import wtf.mxl.pixmix.shared.data.api.PixivAjaxApi
import wtf.mxl.pixmix.shared.data.repository.DiscoveryRepository
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.data.repository.RankingRepository
import wtf.mxl.pixmix.shared.data.repository.SearchRepository
import wtf.mxl.pixmix.shared.network.buildPixivHttpClient
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.network.csrf.CsrfTokenProvider
import wtf.mxl.pixmix.shared.prefs.UserPrefs

val sharedModule = module {
    single { SessionStore(get()) }
    single { UserPrefs(get()) }
    single { PersistentCookiesStorage(get()) }
    single {
        val prefs: UserPrefs = get()
        buildPixivHttpClient(
            cookies = get(),
            proxyConfig = { prefs.proxy.value },
            enableLogging = true,
        )
    }
    single { CsrfTokenProvider(get()) }
    single { PixivAjaxApi(client = get(), csrf = get()) }
    single { DiscoveryRepository(get()) }
    single { IllustRepository(api = get(), csrf = get()) }
    single { SearchRepository(get()) }
    single { RankingRepository(get()) }
}
