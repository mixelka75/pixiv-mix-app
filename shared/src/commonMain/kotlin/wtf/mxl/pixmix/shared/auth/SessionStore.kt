package wtf.mxl.pixmix.shared.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import wtf.mxl.pixmix.shared.platform.SecureStorage

private const val KEY_PHPSESSID = "phpsessid"
private const val KEY_USER_ID = "user_id"

data class Session(val phpSessId: String, val userId: String?)

class SessionStore(private val storage: SecureStorage) {

    private val _session = MutableStateFlow(load())
    val session: StateFlow<Session?> = _session.asStateFlow()

    fun isLoggedIn(): Boolean = _session.value != null

    fun save(phpSessId: String, userId: String?) {
        storage.putString(KEY_PHPSESSID, phpSessId)
        if (userId != null) storage.putString(KEY_USER_ID, userId)
        _session.value = Session(phpSessId, userId)
    }

    fun clear() {
        storage.clear()
        _session.value = null
    }

    private fun load(): Session? {
        val phpSessId = storage.getString(KEY_PHPSESSID) ?: return null
        return Session(phpSessId, storage.getString(KEY_USER_ID))
    }
}
