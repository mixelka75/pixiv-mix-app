package wtf.mxl.pixmix.shared.network.cookies

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import wtf.mxl.pixmix.shared.platform.SecureStorage

private const val KEY_COOKIES = "ktor_cookies_v1"
private const val DOMAIN = "pixiv.net"

/**
 * Stores cookies for *.pixiv.net in [SecureStorage] so they survive process death.
 * In-memory list is the source of truth at runtime; persistence happens on each mutation.
 */
class PersistentCookiesStorage(
    private val storage: SecureStorage,
) : CookiesStorage {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val cookies: MutableList<StoredCookie> = load().toMutableList()

    /** Pre-built `Cookie:` header value for the canonical pixiv origin. Coil fires
     *  many parallel image requests; routing each through `mutex.withLock` to rebuild
     *  this string was a measurable scroll-jank source on Android. We rebuild it only
     *  on mutation. The `@Volatile` is a no-op on Kotlin/Js but keeps JVM/Native sane. */
    @kotlin.concurrent.Volatile
    private var pixivCookieHeaderCached: String = buildPixivCookieHeader()

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        cookies
            .filter { it.matches(requestUrl) }
            .map { it.toKtor() }
    }

    /** Fast-path used by the HTTP interceptor — returns the pre-built `name=value; ...`
     *  string for the canonical pixiv reference URL without entering the mutex. */
    fun pixivCookieHeader(): String = pixivCookieHeaderCached

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.value.isBlank() && cookie.expires == null) return
        mutex.withLock {
            val effDomain = effectiveDomain(cookie, requestUrl)
            val effPath = cookie.path?.takeIf { it.isNotEmpty() } ?: "/"
            // Pixiv issues multiple cookies with the same name but different paths
            // (e.g. `/` vs `/users/`). Dedup must include the path or we collapse
            // them and break per-path scoping.
            cookies.removeAll {
                it.name == cookie.name && it.domain == effDomain && it.path == effPath
            }
            cookies += StoredCookie.from(cookie, requestUrl)
            persist()
            pixivCookieHeaderCached = buildPixivCookieHeader()
        }
    }

    override fun close() = Unit

    /** Convenience for seeding a single cookie (e.g. after WebView login). */
    suspend fun seed(name: String, value: String) {
        addCookie(
            Url("https://www.$DOMAIN/"),
            Cookie(name = name, value = value, domain = ".$DOMAIN", path = "/"),
        )
    }

    suspend fun clear() = mutex.withLock {
        cookies.clear()
        persist()
        pixivCookieHeaderCached = ""
    }

    private fun persist() {
        // Wrap in try/catch — on web `localStorage` throws QuotaExceededError when
        // the 5MB quota is hit (pixiv rotates `__cf_bm` etc, the list grows over time).
        // Dropping persistence is far better than crashing the app to a blank page.
        runCatching {
            storage.putString(KEY_COOKIES, json.encodeToString(ListSerializer(StoredCookie.serializer()), cookies))
        }
    }

    private fun buildPixivCookieHeader(): String {
        val ref = Url("https://www.$DOMAIN/")
        return cookies.filter { it.matches(ref) }.joinToString("; ") { "${it.name}=${it.value}" }
    }

    private fun load(): List<StoredCookie> {
        val raw = storage.getString(KEY_COOKIES) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoredCookie.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun effectiveDomain(cookie: Cookie, requestUrl: Url): String =
        cookie.domain?.removePrefix(".") ?: requestUrl.host

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
        val expiresEpochMs: Long?,
        val secure: Boolean,
        val httpOnly: Boolean,
    ) {
        fun matches(url: Url): Boolean {
            val host = url.host
            if (!host.endsWith(domain)) return false
            if (!url.encodedPath.startsWith(path)) return false
            return true
        }

        fun toKtor(): Cookie = Cookie(
            name = name,
            value = value,
            domain = ".$domain",
            path = path,
            expires = expiresEpochMs?.let { GMTDate(it) },
            secure = secure,
            httpOnly = httpOnly,
        )

        companion object {
            fun from(cookie: Cookie, requestUrl: Url): StoredCookie = StoredCookie(
                name = cookie.name,
                value = cookie.value,
                domain = cookie.domain?.removePrefix(".") ?: requestUrl.host,
                path = cookie.path?.takeIf { it.isNotEmpty() } ?: "/",
                expiresEpochMs = cookie.expires?.timestamp,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
            )
        }
    }
}
