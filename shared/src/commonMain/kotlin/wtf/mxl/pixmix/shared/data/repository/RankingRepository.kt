package wtf.mxl.pixmix.shared.data.repository

import wtf.mxl.pixmix.shared.data.api.PixivAjaxApi
import wtf.mxl.pixmix.shared.data.api.dto.RankingItem
import wtf.mxl.pixmix.shared.domain.model.AuthorSummary
import wtf.mxl.pixmix.shared.domain.model.IllustKind
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.domain.model.XRestrict

class RankingRepository(private val api: PixivAjaxApi) {

    suspend fun ranking(
        mode: String = "daily",
        content: String = "all",
        date: String? = null,
        page: Int = 1,
    ): Result<List<IllustSummary>> = wtf.mxl.pixmix.shared.util.runCoroutineCatching {
        api.ranking(mode = mode, content = content, date = date, page = page)
            .contents.map { it.toSummary() }
    }
}

private fun RankingItem.toSummary(): IllustSummary = IllustSummary(
    id = illust_id.toString(),
    title = title,
    kind = when (illust_type) {
        "1" -> IllustKind.Manga
        "2" -> IllustKind.Ugoira
        else -> IllustKind.Illust
    },
    xRestrict = when {
        attr?.contains("R-18G", ignoreCase = true) == true -> XRestrict.R18G
        attr?.contains("R-18", ignoreCase = true) == true -> XRestrict.R18
        else -> XRestrict.Safe
    },
    width = width,
    height = height,
    pageCount = illust_page_count.toIntOrNull() ?: 1,
    thumbnailUrl = url,
    tags = tags,
    author = AuthorSummary(id = user_id.toString(), name = user_name, avatarUrl = profile_img),
    isMasked = false,
)
