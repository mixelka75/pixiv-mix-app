package wtf.mxl.pixmix.shared.data.api.dto

import kotlinx.serialization.Serializable

/** Thumbnail variant returned in feed/discovery/search responses. */
@Serializable
data class IllustThumbnailDto(
    val id: String,
    val title: String = "",
    val illustType: Int = 0,            // 0 = illust, 1 = manga, 2 = ugoira
    val xRestrict: Int = 0,             // 0 = safe, 1 = R-18, 2 = R-18G
    val restrict: Int = 0,
    val sl: Int = 0,
    val url: String = "",               // square 250 thumb
    val width: Int = 0,
    val height: Int = 0,
    val pageCount: Int = 1,
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val tags: List<String> = emptyList(),
    val isBookmarkable: Boolean = true,
    val isMasked: Boolean = false,
    val aiType: Int = 0,
)
