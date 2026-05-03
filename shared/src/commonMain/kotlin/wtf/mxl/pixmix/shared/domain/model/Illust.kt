package wtf.mxl.pixmix.shared.domain.model

enum class IllustKind { Illust, Manga, Ugoira }
enum class XRestrict { Safe, R18, R18G }

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

data class AuthorSummary(
    val id: String,
    val name: String,
    val avatarUrl: String,
)
