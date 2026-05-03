package wtf.mxl.pixmix.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Builds an [HttpClient] using the platform's preferred engine
 * (OkHttp on Android, Darwin on iOS).
 */
expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
