package wtf.mxl.pixmix.shared.feature.bookmarks

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalBookmarksData
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.feature.actions.FeedActionsController
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.prefs.UserPrefs

class BookmarksComponent(
    componentContext: ComponentContext,
    val store: LocalBookmarkStore,
    likeStore: LocalLikeStore,
    private val illustRepo: IllustRepository,
    imageDownloader: ImageDownloader,
    prefs: UserPrefs,
    private val onOpenIllust: (String) -> Unit,
) : ComponentContext by componentContext {

    val data: StateFlow<LocalBookmarksData> = store.state
    val layout: StateFlow<FeedLayout> = prefs.feedLayout

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    /** null = top-level folder list; otherwise the user is inside that folder. */
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    val actions: FeedActionsController = FeedActionsController(
        likeStore = likeStore,
        bookmarkStore = store,
        repo = illustRepo,
        downloader = imageDownloader,
        scope = scope,
    )

    private val hydrating = mutableSetOf<String>()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        hydrateMissing()
    }

    /**
     * Bookmarks created before the local snapshot cache existed (or whose snapshot got
     * lost) have no [SavedIllust] — we lazily fetch them from `/ajax/illust/{id}` so the
     * Bookmarks tab still renders covers and titles.
     */
    fun hydrateMissing() {
        scope.launch {
            val data = store.state.value
            val missing = (data.items.keys - data.saved.keys) - hydrating
            if (missing.isEmpty()) return@launch
            hydrating += missing
            try {
                missing.forEach { id ->
                    val res = illustRepo.detail(id)
                    res.onSuccess { detail ->
                        store.cacheSnapshot(
                            IllustSummary(
                                id = detail.id,
                                title = detail.title,
                                kind = detail.kind,
                                xRestrict = detail.xRestrict,
                                width = detail.width,
                                height = detail.height,
                                pageCount = detail.pageCount,
                                thumbnailUrl = detail.previewUrl,
                                tags = detail.tags,
                                author = detail.author,
                                isMasked = false,
                            ),
                        )
                    }
                }
            } finally {
                hydrating -= missing
            }
        }
    }

    fun openFolder(id: String) {
        _selectedFolderId.value = id
        hydrateMissing()
    }
    fun back(): Boolean {
        if (_selectedFolderId.value == null) return false
        _selectedFolderId.value = null
        return true
    }

    fun deleteFolder(id: String) {
        if (_selectedFolderId.value == id) _selectedFolderId.value = null
        store.deleteFolder(id)
    }

    fun openIllust(id: String) = onOpenIllust(id)
}
