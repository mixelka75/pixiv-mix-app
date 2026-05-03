package wtf.mxl.pixmix.shared.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

/**
 * Desktop has no embedded webview by default and shipping CEF/JavaFX-WebView would
 * 6× the binary size. Instead we ask the OS to open pixiv in the user's real browser
 * and let them paste the resulting PHPSESSID into the form on the previous screen.
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
            "Sign in via your browser",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Desktop builds don't ship an embedded webview. Open pixiv in your default browser, " +
                "sign in (Google works there), then copy the PHPSESSID cookie and paste it on the " +
                "previous screen.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            runCatching { Desktop.getDesktop().browse(URI(url)) }
        }) {
            Text("Open pixiv in browser")
        }
        OutlinedButton(onClick = {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(url), null)
        }) {
            Text("Copy URL to clipboard")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "How to find PHPSESSID:\n" +
                "1) Sign in on https://www.pixiv.net/\n" +
                "2) Open DevTools (F12) → Application/Storage → Cookies → www.pixiv.net\n" +
                "3) Copy the value of the PHPSESSID row (looks like 12345_AbCdEf…)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
