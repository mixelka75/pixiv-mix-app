package wtf.mxl.pixmix.shared.feature.illust

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
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.domain.model.IllustDetail
import wtf.mxl.pixmix.shared.domain.model.IllustPage
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.feature.actions.FeedActionsController
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.prefs.UserPrefs

class IllustDetailComponent(
    componentContext: ComponentContext,
    val illustId: String,
    private val repo: IllustRepository,
    prefs: UserPrefs,
    likeStore: LocalLikeStore,
    bookmarkStore: LocalBookmarkStore,
    imageDownloader: ImageDownloader,
    private val onOpenViewer: (illustId: String, startIndex: Int) -> Unit,
    private val onOpenIllust: (String) -> Unit,
    private val onBack: () -> Unit,
) : ComponentContext by componentContext {

    data class State(
        val detail: IllustDetail? = null,
        val pages: List<IllustPage> = emptyList(),
        val related: List<IllustSummary> = emptyList(),
        val loading: Boolean = true,
        val loadingRelated: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val layout: StateFlow<FeedLayout> = prefs.feedLayout

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    val actions: FeedActionsController = FeedActionsController(
        likeStore = likeStore,
        bookmarkStore = bookmarkStore,
        repo = repo,
        downloader = imageDownloader,
        scope = scope,
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        load()
    }

    fun toIllustSummary(): IllustSummary? {
        val d = _state.value.detail ?: return null
        return IllustSummary(
            id = d.id,
            title = d.title,
            kind = d.kind,
            xRestrict = d.xRestrict,
            width = d.width,
            height = d.height,
            pageCount = d.pageCount,
            thumbnailUrl = d.previewUrl,
            tags = d.tags,
            author = d.author,
            isMasked = false,
        )
    }

    fun retry() = load()
    fun back() = onBack()
    fun openViewer(index: Int) = onOpenViewer(illustId, index)
    fun openIllust(id: String) = onOpenIllust(id)

    fun toggleLike() {
        val current = _state.value.detail ?: return
        if (current.isLiked) return
        scope.launch {
            val res = repo.like(illustId)
            res.onSuccess { _state.value = _state.value.copy(detail = current.copy(isLiked = true)) }
        }
    }

    fun toggleBookmark(private: Boolean = false) {
        val current = _state.value.detail ?: return
        scope.launch {
            if (current.isBookmarked && current.bookmarkId != null) {
                val res = repo.unbookmark(current.bookmarkId)
                res.onSuccess {
                    _state.value = _state.value.copy(
                        detail = current.copy(isBookmarked = false, bookmarkId = null),
                    )
                }
            } else {
                val res = repo.bookmark(illustId, private)
                res.onSuccess { newId ->
                    _state.value = _state.value.copy(
                        detail = current.copy(isBookmarked = true, bookmarkId = newId),
                    )
                }
            }
        }
    }

    private fun load() {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val detailRes = repo.detail(illustId)
            val pagesRes = repo.pages(illustId)
            _state.value = when {
                detailRes.isSuccess && pagesRes.isSuccess -> _state.value.copy(
                    detail = detailRes.getOrThrow(),
                    pages = pagesRes.getOrThrow(),
                    loading = false,
                )
                else -> _state.value.copy(
                    loading = false,
                    error = (detailRes.exceptionOrNull() ?: pagesRes.exceptionOrNull())?.message
                        ?: "load failed",
                )
            }
            // Fire-and-forget related loading
            launch {
                _state.value = _state.value.copy(loadingRelated = true)
                val res = repo.related(illustId)
                _state.value = _state.value.copy(
                    loadingRelated = false,
                    related = res.getOrNull().orEmpty(),
                )
            }
        }
    }
}
