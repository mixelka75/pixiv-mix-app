package wtf.mxl.pixmix.shared.feature.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wtf.mxl.pixmix.shared.auth.SessionStore
import wtf.mxl.pixmix.shared.data.api.FeedMode
import wtf.mxl.pixmix.shared.data.local.LocalBookmarkStore
import wtf.mxl.pixmix.shared.data.local.LocalLikeStore
import wtf.mxl.pixmix.shared.data.repository.DiscoveryRepository
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.feature.actions.FeedActionsController
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.prefs.UserPrefs

class HomeComponent(
    componentContext: ComponentContext,
    private val discovery: DiscoveryRepository,
    private val sessionStore: SessionStore,
    private val cookies: PersistentCookiesStorage,
    private val prefs: UserPrefs,
    likeStore: LocalLikeStore,
    bookmarkStore: LocalBookmarkStore,
    illustRepo: IllustRepository,
    imageDownloader: ImageDownloader,
    private val onOpenIllust: (String) -> Unit,
) : ComponentContext by componentContext {

    data class State(
        val mode: FeedMode = FeedMode.Safe,
        val items: List<IllustSummary> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val layout: StateFlow<FeedLayout> = prefs.feedLayout

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var loadJob: Job? = null

    val actions: FeedActionsController = FeedActionsController(
        likeStore = likeStore,
        bookmarkStore = bookmarkStore,
        repo = illustRepo,
        downloader = imageDownloader,
        scope = scope,
    )

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        load(reset = true)
    }

    fun openIllust(id: String) = onOpenIllust(id)

    fun setMode(mode: FeedMode) {
        if (mode == _state.value.mode) return
        _state.value = _state.value.copy(mode = mode, items = emptyList())
        load(reset = true)
    }

    fun loadMore() {
        if (_state.value.loading) return
        load(reset = false)
    }

    fun refresh() = load(reset = true)

    fun logout() {
        scope.launch { cookies.clear() }
        sessionStore.clear()
    }

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        loadJob = scope.launch {
            val current = _state.value
            _state.value = current.copy(loading = true, error = null)
            val result = discovery.discover(current.mode)
            _state.value = result.fold(
                onSuccess = { fresh ->
                    val merged = if (reset) fresh else mergeUnique(current.items, fresh)
                    _state.value.copy(items = merged, loading = false)
                },
                onFailure = { _state.value.copy(error = it.message ?: "unknown error", loading = false) },
            )
        }
    }

    private fun mergeUnique(existing: List<IllustSummary>, fresh: List<IllustSummary>): List<IllustSummary> {
        if (existing.isEmpty()) return fresh
        val seen = existing.mapTo(HashSet(existing.size)) { it.id }
        val newOnes = fresh.filter { seen.add(it.id) }
        return existing + newOnes
    }
}
