package wtf.mxl.pixmix.shared.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryBody(
    val thumbnails: ThumbnailsContainer = ThumbnailsContainer(),
    val recommendUsers: List<RecommendUser> = emptyList(),
)

@Serializable
data class ThumbnailsContainer(
    val illust: List<IllustThumbnailDto> = emptyList(),
    val novel: List<JsonRaw> = emptyList(),
)

@Serializable
data class RecommendUser(
    val id: String,
    val illustIds: List<String> = emptyList(),
    val novelIds: List<String> = emptyList(),
)

/** Skipped payload — we don't decode novels yet. */
@Serializable
class JsonRaw
