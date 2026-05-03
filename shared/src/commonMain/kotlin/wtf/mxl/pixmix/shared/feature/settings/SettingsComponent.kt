package wtf.mxl.pixmix.shared.feature.settings

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.StateFlow
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.prefs.ProxyConfig
import wtf.mxl.pixmix.shared.prefs.UserPrefs

class SettingsComponent(
    componentContext: ComponentContext,
    private val prefs: UserPrefs,
) : ComponentContext by componentContext {

    val layout: StateFlow<FeedLayout> = prefs.feedLayout
    val proxy: StateFlow<ProxyConfig> = prefs.proxy

    fun setLayout(layout: FeedLayout) = prefs.setFeedLayout(layout)
    fun setProxyEnabled(value: Boolean) = prefs.setProxyEnabled(value)
    fun setProxyBaseUrl(value: String) = prefs.setProxyBaseUrl(value)
    fun setProxyToken(value: String) = prefs.setProxyToken(value)
}
