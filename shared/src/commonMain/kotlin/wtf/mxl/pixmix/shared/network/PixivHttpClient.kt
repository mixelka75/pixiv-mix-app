package wtf.mxl.pixmix.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.setCookie
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.prefs.ProxyConfig

object PixivHosts {
    const val WEB = "www.pixiv.net"
    const val IMAGE = "i.pximg.net"
    val PIXIV_REFERENCE_URL = Url("https://$WEB/")
}

const val PIXIV_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

val pixivJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
}

fun buildPixivHttpClient(
    cookies: PersistentCookiesStorage,
    proxyConfig: () -> ProxyConfig,
    enableLogging: Boolean = false,
): HttpClient {
    val client = platformHttpClient {
        install(ContentNegotiation) { json(pixivJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 2)
            exponentialDelay()
        }
        if (enableLogging) {
            install(Logging) { level = LogLevel.INFO }
        }

        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                if (host.isEmpty()) host = PixivHosts.WEB
            }
            header(HttpHeaders.UserAgent, PIXIV_USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9,ja;q=0.8")
            header(HttpHeaders.Referrer, "https://${PixivHosts.WEB}/")
            header("Sec-Fetch-Site", "same-origin")
            header("Sec-Fetch-Mode", "cors")
            header("Sec-Fetch-Dest", "empty")
        }
    }

    // Single interception point: (1) attach pixiv cookies, (2) optionally rewrite URL
    // to the user's proxy, (3) on response, capture Set-Cookie back under pixiv.net.
    // Doing cookies manually means proxy on/off is transparent: cookies are always stored
    // and matched against the canonical pixiv.net origin even when the wire host is the proxy.
    client.plugin(HttpSend).intercept { request ->
        val pixivCookies = cookies.get(PixivHosts.PIXIV_REFERENCE_URL)
        if (pixivCookies.isNotEmpty()) {
            request.headers.remove(HttpHeaders.Cookie)
            request.headers.append(
                HttpHeaders.Cookie,
                pixivCookies.joinToString("; ") { "${it.name}=${it.value}" },
            )
        }

        val cfg = proxyConfig()
        if (cfg.enabled && cfg.baseUrl.isNotBlank()) applyProxyRewrite(request, cfg)

        val call = execute(request)

        call.response.setCookie().forEach { responseCookie ->
            cookies.addCookie(
                requestUrl = PixivHosts.PIXIV_REFERENCE_URL,
                cookie = responseCookie.copy(domain = ".${PixivHosts.WEB.removePrefix("www.")}"),
            )
        }
        call
    }

    return client
}

private fun applyProxyRewrite(request: HttpRequestBuilder, cfg: ProxyConfig) {
    val originalHost = request.url.host
    val pathPrefix = when (originalHost) {
        PixivHosts.WEB -> "pixiv"
        PixivHosts.IMAGE -> "img"
        else -> return // leave non-pixiv URLs alone
    }
    val proxyUrl = runCatching { Url(cfg.baseUrl) }.getOrNull() ?: return
    request.url.protocol = proxyUrl.protocol
    request.url.host = proxyUrl.host
    request.url.port = if (proxyUrl.port == 0) proxyUrl.protocol.defaultPort else proxyUrl.port

    // Prepend the prefix while preserving the existing path. pathSegments uses
    // a leading empty string to mean "leading slash".
    val existing = request.url.pathSegments.dropWhile { it.isEmpty() }
    request.url.pathSegments = listOf("", pathPrefix) + existing

    if (cfg.token.isNotBlank()) {
        request.headers.remove("X-Pixmix-Token")
        request.headers.append("X-Pixmix-Token", cfg.token)
    }
}
