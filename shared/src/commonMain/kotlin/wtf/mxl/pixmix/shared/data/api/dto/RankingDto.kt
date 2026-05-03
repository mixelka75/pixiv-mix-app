package wtf.mxl.pixmix.shared.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `ranking.php?format=json` returns a flat object (not the standard ajax envelope).
 */
@Serializable
data class RankingResponse(
    val contents: List<RankingItem> = emptyList(),
    val mode: String = "",
    val content: String = "",
    val page: Int = 1,
    val prev: Boolean = false,
    val next: Int? = null,
    val date: String = "",
    val prev_date: String? = null,
    val next_date: String? = null,
    val rank_total: Int = 0,
)

@Serializable
data class RankingItem(
    val title: String = "",
    val date: String = "",
    val tags: List<String> = emptyList(),
    val url: String = "",
    val illust_type: String = "0",
    val illust_book_style: String = "0",
    val illust_page_count: String = "1",
    val user_name: String = "",
    val profile_img: String = "",
    val illust_content_type: ContentType = ContentType(),
    val illust_id: Long,
    val width: Int = 0,
    val height: Int = 0,
    val user_id: Long = 0,
    val rank: Int = 0,
    val yes_rank: Int = 0,
    val rating_count: Int = 0,
    val view_count: Int = 0,
    val illust_upload_timestamp: Long = 0,
    val attr: String? = null,
)

@Serializable
data class ContentType(
    val sexual: Int = 0,
    val lo: Boolean = false,
    val grotesque: Boolean = false,
    val violent: Boolean = false,
    val homosexual: Boolean = false,
    val drug: Boolean = false,
    val thoughts: Boolean = false,
    val antisocial: Boolean = false,
    val religion: Boolean = false,
    val original: Boolean = false,
    val furry: Boolean = false,
    val bl: Boolean = false,
    val yuri: Boolean = false,
)
