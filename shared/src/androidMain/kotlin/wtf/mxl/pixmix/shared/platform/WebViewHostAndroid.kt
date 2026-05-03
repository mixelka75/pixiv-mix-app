package wtf.mxl.pixmix.shared.platform

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebViewHost(
    url: String,
    modifier: Modifier,
    onCookies: (List<WebCookie>) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CookieManager.getInstance().setAcceptCookie(true)
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = false

                    override fun onPageFinished(view: WebView, finishedUrl: String) {
                        super.onPageFinished(view, finishedUrl)
                        emitCookies(finishedUrl, onCookies)
                    }
                }
                loadUrl(url)
            }
        },
        update = { /* no-op */ },
        onRelease = { it.destroy() },
    )
}

private fun emitCookies(url: String, onCookies: (List<WebCookie>) -> Unit) {
    val raw = CookieManager.getInstance().getCookie(url) ?: return
    val cookies = raw.split(";").mapNotNull { entry ->
        val pair = entry.trim().split("=", limit = 2)
        if (pair.size != 2 || pair[0].isBlank()) return@mapNotNull null
        WebCookie(name = pair[0].trim(), value = pair[1].trim(), domain = ".pixiv.net")
    }
    if (cookies.isNotEmpty()) onCookies(cookies)
}
