package wtf.mxl.pixmix.shared.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random
import wtf.mxl.pixmix.shared.domain.model.AuthorSummary
import wtf.mxl.pixmix.shared.domain.model.IllustKind
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.domain.model.XRestrict

@Serializable
data class BookmarkFolder(
    val id: String,
    val name: String,
)

/**
 * Snapshot of an illust as it looked when the user bookmarked it. Lets us render the
 * Bookmarks tab fully offline (covers, titles, page count, R-18 badges) without re-hitting
 * pixiv. Mirrors [IllustSummary] but stays serializable.
 */
@Serializable
data class SavedIllust(
    val id: String,
    val title: String,
    val kind: String,
    val xRestrict: String,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    val thumbnailUrl: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
) {
    fun toSummary(): IllustSummary = IllustSummary(
        id = id,
        title = title,
        kind = runCatching { IllustKind.valueOf(kind) }.getOrDefault(IllustKind.Illust),
        xRestrict = runCatching { XRestrict.valueOf(xRestrict) }.getOrDefault(XRestrict.Safe),
        width = width,
        height = height,
        pageCount = pageCount,
        thumbnailUrl = thumbnailUrl,
        tags = emptyList(),
        author = AuthorSummary(authorId, authorName, authorAvatarUrl),
        isMasked = false,
    )

    companion object {
        fun fromSummary(s: IllustSummary): SavedIllust = SavedIllust(
            id = s.id,
            title = s.title,
            kind = s.kind.name,
            xRestrict = s.xRestrict.name,
            width = s.width,
            height = s.height,
            pageCount = s.pageCount,
            thumbnailUrl = s.thumbnailUrl,
            authorId = s.author.id,
            authorName = s.author.name,
            authorAvatarUrl = s.author.avatarUrl,
        )
    }
}

@Serializable
data class LocalBookmarksData(
    val folders: List<BookmarkFolder> = emptyList(),
    /** illustId -> set of folderIds */
    val items: Map<String, Set<String>> = emptyMap(),
    /** illustId -> pixiv bookmarkId, when user opted to also sync to pixiv */
    val pixivBookmarkIds: Map<String, String> = emptyMap(),
    /** Minimal info needed to render the Bookmarks tab. */
    val saved: Map<String, SavedIllust> = emptyMap(),
)

/**
 * Local Pinterest-style folders for bookmarked illusts. Independent from pixiv's own
 * bookmark system; the user can optionally also push the bookmark to pixiv (handled
 * by the caller, which stores the resulting bookmarkId here so we know what to delete
 * if they later un-sync).
 */
class LocalBookmarkStore(private val settings: Settings) {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<LocalBookmarksData> = _state.asStateFlow()

    fun isBookmarked(illustId: String): Boolean =
        _state.value.items[illustId]?.isNotEmpty() == true

    fun foldersOf(illustId: String): Set<String> =
        _state.value.items[illustId].orEmpty()

    fun pixivBookmarkId(illustId: String): String? =
        _state.value.pixivBookmarkIds[illustId]

    fun createFolder(name: String): BookmarkFolder {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "folder name must not be empty" }
        val existing = _state.value.folders.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing
        val folder = BookmarkFolder(
            id = "f_${Random.nextLong().toULong().toString(16)}",
            name = trimmed,
        )
        update { it.copy(folders = it.folders + folder) }
        return folder
    }

    fun setFolders(illustId: String, folderIds: Set<String>, snapshot: IllustSummary? = null) {
        update { data ->
            val items = data.items.toMutableMap()
            val saved = data.saved.toMutableMap()
            if (folderIds.isEmpty()) {
                items.remove(illustId)
                saved.remove(illustId)
            } else {
                items[illustId] = folderIds
                if (snapshot != null) saved[illustId] = SavedIllust.fromSummary(snapshot)
            }
            data.copy(items = items, saved = saved)
        }
    }

    /** Posts that live in the given folder, newest-first. */
    fun illustsInFolder(folderId: String): List<IllustSummary> {
        val data = _state.value
        return data.items.entries
            .filter { (_, folderIds) -> folderId in folderIds }
            .mapNotNull { (illustId, _) -> data.saved[illustId]?.toSummary() }
    }

    fun illustCountInFolder(folderId: String): Int =
        _state.value.items.values.count { folderId in it }

    fun deleteFolder(folderId: String) {
        update { data ->
            val items = data.items.mapValues { (_, ids) -> ids - folderId }
                .filterValues { it.isNotEmpty() }
            // Drop saved snapshots that no longer belong to any folder.
            val orphans = data.items.keys - items.keys
            data.copy(
                folders = data.folders.filterNot { it.id == folderId },
                items = items,
                saved = data.saved - orphans,
            )
        }
    }

    fun removeFromAllFolders(illustId: String) {
        update {
            val items = it.items.toMutableMap().apply { remove(illustId) }
            val pixivIds = it.pixivBookmarkIds.toMutableMap().apply { remove(illustId) }
            val saved = it.saved.toMutableMap().apply { remove(illustId) }
            it.copy(items = items, pixivBookmarkIds = pixivIds, saved = saved)
        }
    }

    fun cacheSnapshot(snapshot: IllustSummary) {
        update {
            // Only persist if this illust is actually bookmarked locally — otherwise
            // we'd grow `saved` unboundedly with nothing pointing to it.
            if (snapshot.id !in it.items.keys) return@update it
            it.copy(saved = it.saved + (snapshot.id to SavedIllust.fromSummary(snapshot)))
        }
    }

    fun rememberPixivBookmark(illustId: String, bookmarkId: String?) {
        update {
            val ids = it.pixivBookmarkIds.toMutableMap()
            if (bookmarkId == null) ids.remove(illustId) else ids[illustId] = bookmarkId
            it.copy(pixivBookmarkIds = ids)
        }
    }

    private fun update(block: (LocalBookmarksData) -> LocalBookmarksData) {
        val next = block(_state.value)
        _state.value = next
        settings.putString(KEY, JSON.encodeToString(LocalBookmarksData.serializer(), next))
    }

    private fun load(): LocalBookmarksData {
        val raw = settings.getStringOrNull(KEY) ?: return LocalBookmarksData()
        return runCatching { JSON.decodeFromString(LocalBookmarksData.serializer(), raw) }
            .getOrElse { LocalBookmarksData() }
    }

    companion object {
        private const val KEY = "local_bookmarks_v1"
        private val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

private fun Settings.getStringOrNull(key: String): String? =
    if (hasKey(key)) getString(key, "") else null
