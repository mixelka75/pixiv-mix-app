package wtf.mxl.pixmix.shared.feature.login

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
import wtf.mxl.pixmix.shared.auth.SessionStore
import wtf.mxl.pixmix.shared.network.cookies.PersistentCookiesStorage
import wtf.mxl.pixmix.shared.platform.WebCookie

class LoginComponent(
    componentContext: ComponentContext,
    private val sessionStore: SessionStore,
    private val cookies: PersistentCookiesStorage,
) : ComponentContext by componentContext {

    enum class Mode { Form, Browser }

    data class State(
        val mode: Mode = Mode.Form,
        val sessionInput: String = "",
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    /** Browser-mode WebView target: pixiv login page that, once authenticated, lands on www.pixiv.net. */
    val browserUrl: String =
        "https://accounts.pixiv.net/login?return_to=https%3A%2F%2Fwww.pixiv.net%2F"

    fun setSessionInput(value: String) {
        _state.value = _state.value.copy(sessionInput = value, error = null)
    }

    fun openBrowser() {
        _state.value = _state.value.copy(mode = Mode.Browser, error = null)
    }

    fun openForm() {
        _state.value = _state.value.copy(mode = Mode.Form, error = null)
    }

    fun submitSession() {
        val raw = _state.value.sessionInput.trim()
        if (raw.isBlank()) {
            _state.value = _state.value.copy(error = "Paste your PHPSESSID value")
            return
        }
        if (!raw.contains('_')) {
            _state.value = _state.value.copy(error = "PHPSESSID must look like <userId>_<random>")
            return
        }
        val userId = raw.substringBefore('_')
        scope.launch {
            cookies.seed("PHPSESSID", raw)
            sessionStore.save(raw, userId)
        }
    }

    /** Browser mode: WebView reports cookies → seed both stores. */
    fun onBrowserCookies(captured: List<WebCookie>) {
        val phpSessId = captured.firstOrNull { it.name == "PHPSESSID" }?.value ?: return
        if (!phpSessId.contains('_')) return
        val userId = phpSessId.substringBefore('_')

        scope.launch {
            captured.forEach { cookies.seed(it.name, it.value) }
            sessionStore.save(phpSessId, userId)
        }
    }
}
