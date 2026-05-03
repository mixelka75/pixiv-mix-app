package wtf.mxl.pixmix.shared.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.data.local.BookmarkFolder
import wtf.mxl.pixmix.shared.domain.model.IllustSummary

/** Result of the bookmark sheet — what the host should persist. */
data class BookmarkSheetResult(
    val selectedFolderIds: Set<String>,
    /** New folder names that don't yet exist; the host should create them. */
    val newFolderNames: List<String>,
    val syncToPixiv: Boolean,
    val pixivPrivate: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkFolderSheet(
    illust: IllustSummary,
    folders: List<BookmarkFolder>,
    initiallySelected: Set<String>,
    initiallySyncedToPixiv: Boolean,
    onDismiss: () -> Unit,
    onSave: (BookmarkSheetResult) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(initiallySelected) }
    var newFolderName by remember { mutableStateOf("") }
    var pendingNewNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var syncToPixiv by remember { mutableStateOf(initiallySyncedToPixiv) }
    var pixivPrivate by remember { mutableStateOf(false) }

    fun addPendingName() {
        val trimmed = newFolderName.trim()
        if (trimmed.isEmpty()) return
        if (folders.any { it.name.equals(trimmed, ignoreCase = true) } ||
            pendingNewNames.any { it.equals(trimmed, ignoreCase = true) }
        ) {
            newFolderName = ""
            return
        }
        pendingNewNames = pendingNewNames + trimmed
        newFolderName = ""
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Сохранить «${illust.title.ifBlank { "Untitled" }}»",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Новая папка") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addPendingName() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = ::addPendingName) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить папку")
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderRow(
                        name = folder.name,
                        checked = folder.id in selected,
                        onToggle = { on ->
                            selected = if (on) selected + folder.id else selected - folder.id
                        },
                    )
                }
                items(pendingNewNames, key = { "new:$it" }) { name ->
                    FolderRow(
                        name = "$name  (новая)",
                        checked = true,
                        onToggle = { on ->
                            if (!on) pendingNewNames = pendingNewNames - name
                        },
                    )
                }
                if (folders.isEmpty() && pendingNewNames.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Пока нет папок. Создайте первую сверху.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = syncToPixiv, onCheckedChange = { syncToPixiv = it })
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Также отметить на pixiv", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Имена папок попадут в pixiv как теги закладки",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            if (syncToPixiv) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = !pixivPrivate, onClick = { pixivPrivate = false })
                    Text("Public", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(selected = pixivPrivate, onClick = { pixivPrivate = true })
                    Text("Private")
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onSave(
                            BookmarkSheetResult(
                                selectedFolderIds = selected,
                                newFolderNames = pendingNewNames,
                                syncToPixiv = syncToPixiv,
                                pixivPrivate = pixivPrivate,
                            ),
                        )
                    },
                ) { Text("Сохранить") }
            }
        }
    }
}

@Composable
private fun FolderRow(name: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}
