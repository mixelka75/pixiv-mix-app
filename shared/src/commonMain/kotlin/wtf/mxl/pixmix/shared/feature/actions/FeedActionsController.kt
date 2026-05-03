package wtf.mxl.pixmix.shared.feature.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalBookmarksData
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.ui.bookmarks.BookmarkSheetResult
import wtf.mxl.pixmix.shared.ui.download.DownloadQuality
import wtf.mxl.pixmix.shared.ui.download.DownloadScope
import wtf.mxl.pixmix.shared.ui.download.DownloadSheetResult
import wtf.mxl.pixmix.shared.util.toMasterPreviewUrl
import wtf.mxl.pixmix.shared.util.toSmallSquareUrl

/**
 * One place where the feed/detail action callbacks become real work — pixiv API hits,
 * MediaStore writes, and local-store mutations. Lives at the Component level so the same
 * instance can be reused across recompositions of its hosting screen.
 */
class FeedActionsController(
    val likeStore: LocalLikeStore,
    val bookmarkStore: LocalBookmarkStore,
    private val repo: IllustRepository,
    private val downloader: ImageDownloader,
    private val scope: CoroutineScope,
) {

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Short status messages — host can render these in a Snackbar. */
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    fun like(illustId: String) {
        if (likeStore.isLiked(illustId)) return
        likeStore.markLiked(illustId)
        scope.launch {
            val res = repo.like(illustId)
            res.onFailure { _toasts.tryEmit("Не удалось поставить лайк: ${it.message}") }
        }
    }

    fun saveBookmark(
        illust: IllustSummary,
        result: BookmarkSheetResult,
        snapshot: LocalBookmarksData,
    ) {
        // 1) Persist new folders, build the resolved set of folder IDs.
        val newIds = result.newFolderNames.map { bookmarkStore.createFolder(it).id }
        val resolvedFolderIds = result.selectedFolderIds + newIds.toSet()

        // 2) Persist the illust ↔ folders mapping (with the snapshot so the Bookmarks
        //    tab can render the cover/title without re-hitting pixiv).
        bookmarkStore.setFolders(illust.id, resolvedFolderIds, snapshot = illust)

        // 3) Reconcile the pixiv bookmark.
        val previousPixivId = snapshot.pixivBookmarkIds[illust.id]
        scope.launch {
            if (result.syncToPixiv) {
                val tagNames = bookmarkStore.state.value.folders
                    .filter { it.id in resolvedFolderIds }
                    .map { it.name }
                val res = repo.bookmark(illust.id, private = result.pixivPrivate, tags = tagNames)
                res.onSuccess { newId ->
                    bookmarkStore.rememberPixivBookmark(illust.id, newId)
                }.onFailure {
                    _toasts.tryEmit("Закладка на pixiv: ${it.message}")
                }
            } else if (previousPixivId != null) {
                val res = repo.unbookmark(previousPixivId)
                res.onSuccess { bookmarkStore.rememberPixivBookmark(illust.id, null) }
                    .onFailure { _toasts.tryEmit("Не удалось снять закладку: ${it.message}") }
            }
        }

        if (resolvedFolderIds.isEmpty()) {
            // User cleared all folders → forget local state entirely.
            bookmarkStore.removeFromAllFolders(illust.id)
        }
    }

    fun download(
        illust: IllustSummary,
        currentPageIndex: Int?,
        result: DownloadSheetResult,
    ) {
        scope.launch {
            val pages: List<Int> = when (result.scope) {
                DownloadScope.OnlyCurrentPage -> listOf(currentPageIndex ?: 0)
                DownloadScope.WholePost -> (0 until illust.pageCount.coerceAtLeast(1)).toList()
            }
            val urls: List<Pair<Int, String>> = when (result.quality) {
                DownloadQuality.Original -> {
                    // Original paths are not derivable from the thumbnail — must hit
                    // /ajax/illust/{id}/pages.
                    val pagesRes = repo.pages(illust.id)
                    val real = pagesRes.getOrNull()
                    if (real == null) {
                        _toasts.tryEmit("Не удалось получить original-ссылки")
                        return@launch
                    }
                    pages.mapNotNull { idx -> real.getOrNull(idx)?.let { idx to it.originalUrl } }
                }
                DownloadQuality.Regular -> pages.map { idx ->
                    idx to illust.thumbnailUrl.toMasterPreviewUrl(pageIndex = idx)
                }
                DownloadQuality.Square -> pages.map { idx ->
                    idx to illust.thumbnailUrl.toSmallSquareUrl(pageIndex = idx)
                }
            }

            _toasts.tryEmit("Скачиваю ${urls.size} стр.…")
            var ok = 0
            urls.forEach { (idx, url) ->
                val ext = extOf(url)
                val name = "${illust.id}_p${idx}.${ext}"
                val res = downloader.saveImage(url, name)
                if (res.isSuccess) ok++ else _toasts.tryEmit("p$idx: ${res.exceptionOrNull()?.message}")
            }
            _toasts.tryEmit("Готово: $ok из ${urls.size}")
        }
    }

    private fun extOf(url: String): String =
        url.substringAfterLast('.', "jpg").substringBefore('?').lowercase().take(4)
}
