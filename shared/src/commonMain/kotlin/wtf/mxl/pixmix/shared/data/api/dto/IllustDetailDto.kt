package wtf.mxl.pixmix.shared.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class IllustDetailDto(
    val id: String,
    val title: String = "",
    val description: String = "",
    val illustType: Int = 0,
    val xRestrict: Int = 0,
    val createDate: String = "",
    val uploadDate: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val pageCount: Int = 1,
    val bookmarkCount: Int = 0,
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val commentCount: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userAccount: String = "",
    val tags: TagContainer = TagContainer(),
    val urls: ImageUrls = ImageUrls(),
    val likeData: Boolean = false,
    val isBookmarked: Boolean = false,
    val bookmarkData: BookmarkData? = null,
    val aiType: Int = 0,
    val isOriginal: Boolean = false,
)

@Serializable
data class TagContainer(
    val tags: List<TagEntry> = emptyList(),
)

@Serializable
data class TagEntry(
    val tag: String,
    val locked: Boolean = false,
    val deletable: Boolean = false,
    val translation: Map<String, String>? = null,
)

@Serializable
data class ImageUrls(
    val mini: String = "",
    val thumb: String = "",
    val small: String = "",
    val regular: String = "",
    val original: String = "",
)

@Serializable
data class BookmarkData(
    val id: String,
    val private: Boolean = false,
)

@Serializable
data class IllustPageDto(
    val urls: ImageUrls,
    val width: Int = 0,
    val height: Int = 0,
)
