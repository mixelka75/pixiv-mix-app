package wtf.mxl.pixmix.shared.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendInitBody(
    val illusts: List<IllustThumbnailDto> = emptyList(),
    val nextIds: List<String> = emptyList(),
)
