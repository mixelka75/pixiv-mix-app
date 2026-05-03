package wtf.mxl.pixmix.shared.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.platform.WebViewHost

@Composable
fun LoginScreen(component: LoginComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state.mode) {
            LoginComponent.Mode.Form -> FormMode(component, state)
            LoginComponent.Mode.Browser -> BrowserMode(component)
        }
    }
}

@Composable
private fun FormMode(component: LoginComponent, state: LoginComponent.State) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("PixMix", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text(
            "Sign in to pixiv",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Paste your PHPSESSID",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Open pixiv.net in your browser → DevTools → Application → Cookies → " +
                "copy the PHPSESSID value (looks like 12345678_AbCdEfGh…).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        OutlinedTextField(
            value = state.sessionInput,
            onValueChange = component::setSessionInput,
            label = { Text("PHPSESSID") },
            placeholder = { Text("e.g. 12345678_AbCdEf…") },
            singleLine = true,
            isError = state.error != null,
            supportingText = state.error?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = component::submitSession,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.sessionInput.isNotBlank(),
        ) {
            Text("Sign in")
        }

        Spacer(Modifier.height(8.dp))
        Text("— or —", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = component::openBrowser,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in via browser (WebView)")
        }
        Text(
            "Use email + password directly on pixiv. Google sign-in does not work in embedded browsers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun BrowserMode(component: LoginComponent) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = component::openForm) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Sign in", style = MaterialTheme.typography.titleMedium)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            WebViewHost(
                url = component.browserUrl,
                modifier = Modifier.fillMaxSize(),
                onCookies = component::onBrowserCookies,
            )
        }
    }
}
