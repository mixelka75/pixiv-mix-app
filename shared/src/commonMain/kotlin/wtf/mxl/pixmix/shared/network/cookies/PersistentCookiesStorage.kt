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

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        cookies
            .filter { it.matches(requestUrl) }
            .map { it.toKtor() }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.value.isBlank() && cookie.expires == null) return
        mutex.withLock {
            cookies.removeAll { it.name == cookie.name && it.domain == effectiveDomain(cookie, requestUrl) }
            cookies += StoredCookie.from(cookie, requestUrl)
            persist()
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
    }

    private fun persist() {
        storage.putString(KEY_COOKIES, json.encodeToString(ListSerializer(StoredCookie.serializer()), cookies))
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
