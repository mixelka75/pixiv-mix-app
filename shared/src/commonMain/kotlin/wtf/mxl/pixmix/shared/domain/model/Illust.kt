package wtf.mxl.pixmix.shared.domain.model

import androidx.compose.runtime.Immutable

enum class IllustKind { Illust, Manga, Ugoira }
enum class XRestrict { Safe, R18, R18G }

@Immutable
data class IllustSummary(
    val id: String,
    val title: String,
    val kind: IllustKind,
    val xRestrict: XRestrict,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    val thumbnailUrl: String,
    val tags: List<String>,
    val author: AuthorSummary,
    val isMasked: Boolean,
)

@Immutable
data class AuthorSummary(
    val id: String,
    val name: String,
    val avatarUrl: String,
)
