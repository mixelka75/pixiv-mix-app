package wtf.mxl.pixmix.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class WebCookie(val name: String, val value: String, val domain: String?)

/**
 * Hosts a system WebView pointed at [url]. Calls [onCookies] each time the page navigation
 * settles, with the full set of cookies for the host. The login flow listens for PHPSESSID
 * to appear and then dismisses itself.
 */
@Composable
expect fun WebViewHost(
    url: String,
    modifier: Modifier = Modifier,
    onCookies: (List<WebCookie>) -> Unit,
)
