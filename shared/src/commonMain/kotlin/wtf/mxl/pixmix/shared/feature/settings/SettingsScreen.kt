package wtf.mxl.pixmix.shared.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.prefs.FeedLayout

@Composable
fun SettingsScreen(component: SettingsComponent, modifier: Modifier = Modifier) {
    val layout by component.layout.collectAsState()
    val proxy by component.proxy.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

        // ---- Feed layout ----
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Feed layout", style = MaterialTheme.typography.titleMedium)
            Text(
                "Grid is dense (3 columns). Feed is full-width with horizontal swipe between pages of multi-page works (VK-style).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                FeedLayout.entries.forEachIndexed { index, opt ->
                    SegmentedButton(
                        selected = opt == layout,
                        onClick = { component.setLayout(opt) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = FeedLayout.entries.size,
                        ),
                        icon = {
                            Icon(
                                imageVector = if (opt == FeedLayout.Grid)
                                    Icons.Filled.GridView
                                else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = null,
                            )
                        },
                    ) { Text(opt.name) }
                }
            }
        }

        HorizontalDivider()

        // ---- Proxy ----
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use proxy", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Route all pixiv traffic through your own nginx (faster than VPN, no RKN block).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Switch(checked = proxy.enabled, onCheckedChange = component::setProxyEnabled)
            }
            OutlinedTextField(
                value = proxy.baseUrl,
                onValueChange = component::setProxyBaseUrl,
                label = { Text("Proxy base URL") },
                placeholder = { Text("https://pixmix.mxl.wtf") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = proxy.enabled,
            )
            OutlinedTextField(
                value = proxy.token,
                onValueChange = component::setProxyToken,
                label = { Text("Proxy token (X-Pixmix-Token)") },
                placeholder = { Text("shared secret matching nginx config") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = proxy.enabled,
            )
            Text(
                "Changes apply to the next request. No need to re-login when toggling — your pixiv session stays valid in either mode.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
