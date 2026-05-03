package wtf.mxl.pixmix.shared.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.feature.actions.FeedActionsController
import wtf.mxl.pixmix.shared.ui.bookmarks.BookmarkFolderSheet
import wtf.mxl.pixmix.shared.ui.download.DownloadSheet

/**
 * Wraps a feed/detail screen with the bookmark and download bottom sheets, plus a
 * snackbar that picks up status messages from [FeedActionsController]. The slot
 * receives a ready-to-use [FeedActions] which the host content forwards to feed
 * composables (or single-illust action rows).
 */
@Composable
fun FeedActionsHost(
    controller: FeedActionsController,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarSlot: @Composable () -> Unit = { SnackbarHost(snackbarHostState, modifier = Modifier) },
    content: @Composable (FeedActions) -> Unit,
) {
    val likedIds by controller.likeStore.state.collectAsState()
    val bookmarkData by controller.bookmarkStore.state.collectAsState()

    var bookmarkSheetFor by remember { mutableStateOf<IllustSummary?>(null) }
    var downloadSheetFor by remember { mutableStateOf<Pair<IllustSummary, Int?>?>(null) }

    LaunchedEffect(controller) {
        controller.toasts.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    val actions = FeedActions(
        likedIds = likedIds,
        bookmarkedIds = bookmarkData.items.keys,
        onLike = controller::like,
        onBookmarkClick = { bookmarkSheetFor = it },
        onDownloadClick = { illust, page -> downloadSheetFor = illust to page },
    )

    content(actions)
    snackbarSlot()

    bookmarkSheetFor?.let { illust ->
        BookmarkFolderSheet(
            illust = illust,
            folders = bookmarkData.folders,
            initiallySelected = bookmarkData.items[illust.id].orEmpty(),
            initiallySyncedToPixiv = bookmarkData.pixivBookmarkIds.containsKey(illust.id),
            onDismiss = { bookmarkSheetFor = null },
            onSave = { result ->
                controller.saveBookmark(illust, result, bookmarkData)
                bookmarkSheetFor = null
            },
        )
    }
    downloadSheetFor?.let { (illust, page) ->
        DownloadSheet(
            illust = illust,
            currentPageIndex = page,
            onDismiss = { downloadSheetFor = null },
            onDownload = { result ->
                controller.download(illust, page, result)
                downloadSheetFor = null
            },
        )
    }
}
