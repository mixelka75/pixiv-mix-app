package wtf.mxl.pixmix.shared.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchArtworksBody(
    val illustManga: SearchSection = SearchSection(),
    val popular: PopularSection = PopularSection(),
)

@Serializable
data class SearchSection(
    val data: List<IllustThumbnailDto> = emptyList(),
    val total: Int = 0,
    val lastPage: Int = 0,
)

@Serializable
data class PopularSection(
    val recent: List<IllustThumbnailDto> = emptyList(),
    val permanent: List<IllustThumbnailDto> = emptyList(),
)
