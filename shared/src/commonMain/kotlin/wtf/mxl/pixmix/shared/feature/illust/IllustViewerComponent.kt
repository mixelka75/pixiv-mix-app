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
import wtf.mxl.pixmix.shared.data.repository.IllustRepository
import wtf.mxl.pixmix.shared.domain.model.IllustPage

class IllustViewerComponent(
    componentContext: ComponentContext,
    val illustId: String,
    val initialIndex: Int,
    private val repo: IllustRepository,
    private val onBack: () -> Unit,
) : ComponentContext by componentContext {

    data class State(
        val pages: List<IllustPage> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        scope.launch {
            val res = repo.pages(illustId)
            _state.value = res.fold(
                onSuccess = { State(pages = it, loading = false) },
                onFailure = { State(loading = false, error = it.message) },
            )
        }
    }

    fun back() = onBack()
}
