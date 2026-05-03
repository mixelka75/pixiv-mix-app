package wtf.mxl.pixmix.shared.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class IllustDetail(
    val id: String,
    val title: String,
    val description: String,
    val kind: IllustKind,
    val xRestrict: XRestrict,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    val createDate: String,
    val viewCount: Int,
    val bookmarkCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val author: AuthorSummary,
    val tags: List<String>,
    val previewUrl: String,
    val regularUrl: String,
    val originalUrl: String,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val bookmarkId: String?,
)

@Immutable
data class IllustPage(
    val regularUrl: String,
    val originalUrl: String,
    val width: Int,
    val height: Int,
)
