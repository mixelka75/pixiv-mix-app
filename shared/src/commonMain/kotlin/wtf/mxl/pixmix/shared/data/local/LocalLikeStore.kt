package wtf.mxl.pixmix.shared.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LikeData(val ids: Set<String> = emptySet())

/**
 * Pixiv's `/ajax/illusts/like` is one-way (no unlike endpoint), and listing endpoints
 * don't tell us which illusts the current user already liked. We persist locally so the
 * heart icon stays filled across app restarts after the user taps it.
 */
class LocalLikeStore(private val settings: Settings) {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Set<String>> = _state.asStateFlow()

    fun isLiked(illustId: String): Boolean = _state.value.contains(illustId)

    fun markLiked(illustId: String) {
        if (_state.value.contains(illustId)) return
        val next = _state.value + illustId
        _state.value = next
        settings.putString(KEY, JSON.encodeToString(LikeData.serializer(), LikeData(next)))
    }

    private fun load(): Set<String> {
        val raw = if (settings.hasKey(KEY)) settings.getString(KEY, "") else null
        if (raw.isNullOrEmpty()) return emptySet()
        return runCatching { JSON.decodeFromString(LikeData.serializer(), raw).ids }
            .getOrElse { emptySet() }
    }

    companion object {
        private const val KEY = "local_likes_v1"
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
