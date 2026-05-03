package wtf.mxl.pixmix.shared.data.repository

import wtf.mxl.pixmix.shared.data.api.FeedMode
import wtf.mxl.pixmix.shared.data.api.PixivAjaxApi
import wtf.mxl.pixmix.shared.data.toDomain
import wtf.mxl.pixmix.shared.domain.model.IllustSummary

class SearchRepository(private val api: PixivAjaxApi) {

    suspend fun searchArtworks(
        keyword: String,
        page: Int = 1,
        mode: FeedMode = FeedMode.All,
        order: String = "date_d",
    ): Result<List<IllustSummary>> = wtf.mxl.pixmix.shared.util.runCoroutineCatching {
        val env = api.searchArtworks(keyword, page = page, mode = mode, order = order)
        if (env.error || env.body == null) error(env.message.ifBlank { "search failed" })
        env.body.illustManga.data.map { it.toDomain() }
    }
}
