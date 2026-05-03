package wtf.mxl.pixmix.shared.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.domain.model.IllustSummary

enum class DownloadQuality(val label: String) {
    Original("Original"),
    Regular("Regular (~1200px)"),
    Square("Square thumbnail"),
}

enum class DownloadScope { WholePost, OnlyCurrentPage }

data class DownloadSheetResult(
    val quality: DownloadQuality,
    val scope: DownloadScope,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(
    illust: IllustSummary,
    currentPageIndex: Int?,
    onDismiss: () -> Unit,
    onDownload: (DownloadSheetResult) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quality by remember { mutableStateOf(DownloadQuality.Original) }
    val multipage = illust.pageCount > 1
    var scope by remember {
        mutableStateOf(
            if (multipage && currentPageIndex != null) DownloadScope.OnlyCurrentPage
            else DownloadScope.WholePost,
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                "Скачать",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))

            Text("Качество", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DownloadQuality.entries.forEachIndexed { index, q ->
                    SegmentedButton(
                        selected = quality == q,
                        onClick = { quality = q },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DownloadQuality.entries.size,
                        ),
                    ) { Text(q.label) }
                }
            }

            if (multipage) {
                Spacer(Modifier.height(16.dp))
                Text("Что качать", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = scope == DownloadScope.WholePost,
                        onClick = { scope = DownloadScope.WholePost },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Весь пост (${illust.pageCount} стр.)") }
                    SegmentedButton(
                        selected = scope == DownloadScope.OnlyCurrentPage,
                        onClick = { scope = DownloadScope.OnlyCurrentPage },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        enabled = currentPageIndex != null,
                    ) {
                        Text(
                            if (currentPageIndex != null)
                                "Только страница ${currentPageIndex + 1}"
                            else "Текущая страница",
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onDownload(DownloadSheetResult(quality, scope))
                    },
                ) { Text("Скачать") }
            }
        }
    }
}
