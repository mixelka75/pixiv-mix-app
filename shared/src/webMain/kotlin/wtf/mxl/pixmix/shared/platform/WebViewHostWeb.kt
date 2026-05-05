package wtf.mxl.pixmix.shared.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.window

/**
 * On web there's no in-app browser/iframe that can capture pixiv cookies — same-origin
 * policy plus pixiv's `X-Frame-Options` rule out an iframe-based login. The realistic flow
 * is: open pixiv in a new tab, sign in there, copy `PHPSESSID` from devtools, paste it
 * back into the previous screen's text field.
 */
@Composable
actual fun WebViewHost(
    url: String,
    modifier: Modifier,
    onCookies: (List<WebCookie>) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Sign in on pixiv.net",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Open pixiv in a new tab, sign in there, then come back and paste your PHPSESSID " +
                "cookie into the form on the previous screen.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { window.open(url, "_blank") }) {
            Text("Open pixiv in new tab")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "How to find PHPSESSID:\n" +
                "1) Sign in on pixiv.net (in the new tab)\n" +
                "2) DevTools (F12) → Application → Cookies → www.pixiv.net\n" +
                "3) Copy the value of PHPSESSID (looks like 12345_AbCdEf…)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
