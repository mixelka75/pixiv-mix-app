package wtf.mxl.pixmix.shared.feature.ranking

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
import wtf.mxl.pixmix.shared.data.repository.RankingRepository
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.prefs.UserPrefs

enum class RankingPeriod(val wire: String) {
    Daily("daily"),
    Weekly("weekly"),
    Monthly("monthly"),
    Rookie("rookie"),
    Original("original"),
    DailyR18("daily_r18"),
    WeeklyR18("weekly_r18"),
}

class RankingComponent(
    componentContext: ComponentContext,
    private val repo: RankingRepository,
    prefs: UserPrefs,
    private val onOpenIllust: (String) -> Unit,
) : ComponentContext by componentContext {

    val layout: StateFlow<FeedLayout> = prefs.feedLayout


    data class State(
        val period: RankingPeriod = RankingPeriod.Daily,
        val items: List<IllustSummary> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val page: Int = 1,
        val endReached: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        load(reset = true)
    }

    fun setPeriod(p: RankingPeriod) {
        if (p == _state.value.period) return
        _state.value = _state.value.copy(period = p, items = emptyList(), page = 1, endReached = false)
        load(reset = true)
    }

    fun openIllust(id: String) = onOpenIllust(id)

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.endReached) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        scope.launch {
            val s = _state.value
            val nextPage = if (reset) 1 else s.page + 1
            _state.value = s.copy(loading = true, error = null)
            val res = repo.ranking(mode = s.period.wire, page = nextPage)
            _state.value = res.fold(
                onSuccess = { fresh ->
                    val merged = if (reset) fresh else s.items + fresh
                    _state.value.copy(
                        items = merged,
                        loading = false,
                        page = nextPage,
                        endReached = fresh.isEmpty(),
                    )
                },
                onFailure = { _state.value.copy(error = it.message, loading = false) },
            )
        }
    }
}
