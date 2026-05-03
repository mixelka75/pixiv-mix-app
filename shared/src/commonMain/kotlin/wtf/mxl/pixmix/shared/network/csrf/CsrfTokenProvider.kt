package wtf.mxl.pixmix.shared.network.csrf

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import wtf.mxl.pixmix.shared.network.PixivHosts

private const val TTL_MS = 25 * 60 * 1000L

/**
 * Pixiv requires the `x-csrf-token` header on all mutating AJAX calls.
 * The token is rendered into the document head as either:
 *   <meta name="global-data" content='{...,"token":"<TOKEN>",...}'>
 * or, on legacy pages, as an input named `tt`. We try both.
 */
class CsrfTokenProvider(private val client: HttpClient) {

    private val mutex = Mutex()
    private var cached: String? = null
    private var cachedAtMs: Long = 0

    suspend fun get(forceRefresh: Boolean = false): String? = mutex.withLock {
        val now = getTimeMillis()
        val current = cached
        if (!forceRefresh && current != null && now - cachedAtMs < TTL_MS) return current

        val html = runCatching {
            client.get("https://${PixivHosts.WEB}/").bodyAsText()
        }.getOrNull() ?: return current

        val token = extractToken(html)
        if (token != null) {
            cached = token
            cachedAtMs = now
        }
        token ?: current
    }

    fun invalidate() {
        cached = null
        cachedAtMs = 0
    }
}

internal fun extractToken(html: String): String? {
    val metaRegex = Regex(
        "<meta[^>]+name=\"global-data\"[^>]+content=(['\"])(.*?)\\1",
        RegexOption.DOT_MATCHES_ALL,
    )
    metaRegex.find(html)?.groupValues?.get(2)?.let { contentRaw ->
        val unescaped = contentRaw
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
        Regex("\"token\"\\s*:\\s*\"([^\"]+)\"").find(unescaped)?.groupValues?.get(1)?.let { return it }
    }
    Regex("<input[^>]+name=\"tt\"[^>]+value=\"([^\"]+)\"").find(html)
        ?.groupValues?.get(1)?.let { return it }
    return null
}
