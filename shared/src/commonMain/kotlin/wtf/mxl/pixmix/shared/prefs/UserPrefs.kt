package wtf.mxl.pixmix.shared.prefs

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FeedLayout { Grid, Feed }

data class ProxyConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val token: String = "",
)

class UserPrefs(private val settings: Settings) {

    private val _feedLayout = MutableStateFlow(loadLayout())
    val feedLayout: StateFlow<FeedLayout> = _feedLayout.asStateFlow()

    private val _proxy = MutableStateFlow(loadProxy())
    val proxy: StateFlow<ProxyConfig> = _proxy.asStateFlow()

    fun setFeedLayout(layout: FeedLayout) {
        settings.putString(KEY_FEED_LAYOUT, layout.name)
        _feedLayout.value = layout
    }

    fun setProxyEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_PROXY_ENABLED, enabled)
        _proxy.value = _proxy.value.copy(enabled = enabled)
    }

    fun setProxyBaseUrl(url: String) {
        val normalized = normalizeBaseUrl(url)
        settings.putString(KEY_PROXY_BASE, normalized)
        _proxy.value = _proxy.value.copy(baseUrl = normalized)
    }

    private fun normalizeBaseUrl(url: String): String {
        val cleaned = url.trim().trimEnd('/')
        return when {
            cleaned.isBlank() -> ""
            cleaned.startsWith("http://", ignoreCase = true) -> cleaned
            cleaned.startsWith("https://", ignoreCase = true) -> cleaned
            else -> "https://$cleaned"
        }
    }

    fun setProxyToken(token: String) {
        val trimmed = token.trim()
        settings.putString(KEY_PROXY_TOKEN, trimmed)
        _proxy.value = _proxy.value.copy(token = trimmed)
    }

    private fun loadLayout(): FeedLayout = runCatching {
        FeedLayout.valueOf(settings.getString(KEY_FEED_LAYOUT, FeedLayout.Grid.name))
    }.getOrDefault(FeedLayout.Grid)

    private fun loadProxy(): ProxyConfig = ProxyConfig(
        enabled = settings.getBoolean(KEY_PROXY_ENABLED, false),
        baseUrl = normalizeBaseUrl(settings.getString(KEY_PROXY_BASE, "")),
        token = settings.getString(KEY_PROXY_TOKEN, ""),
    )

    private companion object {
        const val KEY_FEED_LAYOUT = "feed_layout"
        const val KEY_PROXY_ENABLED = "proxy_enabled"
        const val KEY_PROXY_BASE = "proxy_base_url"
        const val KEY_PROXY_TOKEN = "proxy_token"
    }
}
