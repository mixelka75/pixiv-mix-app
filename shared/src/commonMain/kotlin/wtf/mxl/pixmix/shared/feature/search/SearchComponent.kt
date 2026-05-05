package wtf.mxl.pixmix.shared.feature.search

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wtf.mxl.pixmix.shared.data.api.FeedMode
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.data.repository.SearchRepository
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.feature.actions.FeedActionsController
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.prefs.UserPrefs

class SearchComponent(
    componentContext: ComponentContext,
    private val repo: SearchRepository,
    prefs: UserPrefs,
    likeStore: LocalLikeStore,
    bookmarkStore: LocalBookmarkStore,
    illustRepo: IllustRepository,
    imageDownloader: ImageDownloader,
    private val onOpenIllust: (String) -> Unit,
) : ComponentContext by componentContext {

    val layout: StateFlow<FeedLayout> = prefs.feedLayout


    data class State(
        val query: String = "",
        val mode: FeedMode = FeedMode.All,
        val items: List<IllustSummary> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val page: Int = 1,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var searchJob: Job? = null
    private var debounceJob: Job? = null

    val actions: FeedActionsController = FeedActionsController(
        likeStore = likeStore,
        bookmarkStore = bookmarkStore,
        repo = illustRepo,
        downloader = imageDownloader,
        scope = scope,
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        // Debounced submit — wait 350ms after the last keystroke before firing the
        // network call. submit() also cancels searchJob so an in-flight request from
        // a previous query won't race with this one.
        debounceJob?.cancel()
        if (q.isBlank()) return
        debounceJob = scope.launch {
            delay(350)
            submit()
        }
    }

    fun clearQuery() {
        debounceJob?.cancel()
        searchJob?.cancel()
        _state.value = _state.value.copy(query = "", items = emptyList(), error = null, page = 1)
    }

    fun setMode(m: FeedMode) {
        _state.value = _state.value.copy(mode = m)
        if (_state.value.query.isNotBlank()) submit()
    }

    fun submit() {
        val s = _state.value
        if (s.query.isBlank()) return
        searchJob?.cancel()
        searchJob = scope.launch {
            _state.value = s.copy(loading = true, error = null, items = emptyList(), page = 1)
            val res = repo.searchArtworks(s.query.trim(), page = 1, mode = s.mode)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            // Abort if a new query was submitted while we were waiting for the old one.
            if (_state.value.query != s.query || _state.value.mode != s.mode) return@launch
            _state.value = res.fold(
                onSuccess = { _state.value.copy(items = it, loading = false) },
                onFailure = { _state.value.copy(error = it.message, loading = false) },
            )
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.query.isBlank()) return
        // Track loadMore in the same searchJob so submit() cancels it — without this
        // an in-flight loadMore can resume after submit() reset items and append page-2
        // results onto the new (empty) state, breaking pagination.
        searchJob?.cancel()
        searchJob = scope.launch {
            _state.value = s.copy(loading = true)
            val res = repo.searchArtworks(s.query.trim(), page = s.page + 1, mode = s.mode)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (_state.value.query != s.query || _state.value.mode != s.mode) return@launch
            _state.value = res.fold(
                onSuccess = { _state.value.copy(items = s.items + it, loading = false, page = s.page + 1) },
                onFailure = { _state.value.copy(loading = false, error = it.message) },
            )
        }
    }

    fun openIllust(id: String) = onOpenIllust(id)

    fun refresh() {
        if (_state.value.query.isNotBlank()) submit()
    }
}
