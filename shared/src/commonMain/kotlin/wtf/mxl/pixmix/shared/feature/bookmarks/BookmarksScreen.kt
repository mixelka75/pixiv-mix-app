package wtf.mxl.pixmix.shared.feature.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.data.local.BookmarkFolder
import wtf.mxl.pixmix.shared.data.local.LocalBookmarksData
import wtf.mxl.pixmix.shared.prefs.BookmarkSort
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.ui.FeedActionsHost
import wtf.mxl.pixmix.shared.ui.IllustFeed
import wtf.mxl.pixmix.shared.ui.IllustGrid
import wtf.mxl.pixmix.shared.ui.IllustTileFeed
import wtf.mxl.pixmix.shared.ui.PixivImage
import wtf.mxl.pixmix.shared.ui.WIDE_FEED_BREAKPOINT
import wtf.mxl.pixmix.shared.util.toMasterPreviewUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(component: BookmarksComponent, modifier: Modifier = Modifier) {
    val data by component.data.collectAsState()
    val layout by component.layout.collectAsState()
    val selectedFolderId by component.selectedFolderId.collectAsState()
    val openFolder = remember(data, selectedFolderId) {
        selectedFolderId?.let { id -> data.folders.firstOrNull { it.id == id } }
    }
    val currentSort by component.sort.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(openFolder?.name ?: "Закладки") },
                navigationIcon = {
                    if (openFolder != null) {
                        IconButton(onClick = { component.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (openFolder != null) {
                        SortMenu(current = currentSort, onSelect = component::setSort)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (openFolder == null) {
                FolderGrid(
                    data = data,
                    onOpen = component::openFolder,
                    onDelete = component::deleteFolder,
                )
            } else {
                FolderContent(
                    component = component,
                    folder = openFolder,
                    layout = layout,
                    sort = currentSort,
                )
            }
        }
    }
}

@Composable
private fun FolderGrid(
    data: LocalBookmarksData,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<BookmarkFolder?>(null) }

    if (data.folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Здесь будут ваши папки. Откройте пост в ленте → закладка → Создать папку.",
                modifier = Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(data.folders, key = { it.id }) { folder ->
            FolderCard(
                folder = folder,
                cover = data.firstCoverUrl(folder.id),
                count = data.itemsInFolder(folder.id),
                onClick = { onOpen(folder.id) },
                onLongPress = { pendingDelete = folder },
            )
        }
    }

    pendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить папку «${folder.name}»?") },
            text = {
                Text(
                    "Сами иллюстрации останутся. На pixiv ничего не меняется.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(folder.id)
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    folder: BookmarkFolder,
    cover: String?,
    count: Int,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222)),
        ) {
            if (cover != null) {
                PixivImage(
                    url = cover,
                    contentDescription = folder.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "$count постов",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = onLongPress, modifier = Modifier.padding(start = 4.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Удалить папку",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun FolderContent(
    component: BookmarksComponent,
    folder: BookmarkFolder,
    layout: FeedLayout,
    sort: BookmarkSort,
) {
    val data by component.data.collectAsState()
    val items = remember(folder.id, data, sort) {
        val raw = component.store.illustsInFolder(folder.id)
        when (sort) {
            // illustsInFolder preserves underlying map insertion order — newest is last,
            // oldest is first. Reverse for "newest first".
            BookmarkSort.Newest -> raw.asReversed()
            BookmarkSort.Oldest -> raw
            BookmarkSort.Name -> raw.sortedBy { it.title.lowercase() }
        }
    }
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "В этой папке пока пусто.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }
    FeedActionsHost(controller = component.actions) { actions ->
        when (layout) {
            FeedLayout.Grid -> IllustGrid(
                items = items,
                onClick = component::openIllust,
                onEndReached = null,
                actions = actions,
            )
            FeedLayout.Feed -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxWidth >= WIDE_FEED_BREAKPOINT) {
                    IllustTileFeed(
                        items = items,
                        onClick = component::openIllust,
                        actions = actions,
                    )
                } else {
                    IllustFeed(
                        items = items,
                        onClick = component::openIllust,
                        actions = actions,
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(current: BookmarkSort, onSelect: (BookmarkSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Сортировка")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BookmarkSort.entries.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(when (opt) {
                            BookmarkSort.Newest -> "Сначала новые"
                            BookmarkSort.Oldest -> "Сначала старые"
                            BookmarkSort.Name -> "По названию"
                        })
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                    trailingIcon = if (opt == current) {
                        { Text("●", color = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}

private fun LocalBookmarksData.itemsInFolder(folderId: String): Int =
    items.values.count { folderId in it }

private fun LocalBookmarksData.firstCoverUrl(folderId: String): String? {
    val firstId = items.entries.firstOrNull { folderId in it.value }?.key ?: return null
    val saved = saved[firstId] ?: return null
    return saved.thumbnailUrl.toMasterPreviewUrl()
}
