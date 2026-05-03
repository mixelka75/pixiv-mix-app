package wtf.mxl.pixmix.shared.data.repository

import wtf.mxl.pixmix.shared.data.api.FeedMode
import wtf.mxl.pixmix.shared.data.api.PixivAjaxApi
import wtf.mxl.pixmix.shared.data.toDomain
import wtf.mxl.pixmix.shared.domain.model.IllustSummary

class DiscoveryRepository(private val api: PixivAjaxApi) {

    suspend fun discover(mode: FeedMode, limit: Int = 60): Result<List<IllustSummary>> = runCatching {
        val env = api.discovery(mode, limit)
        if (env.error) error(env.message.ifBlank { "pixiv returned error" })
        env.body?.thumbnails?.illust.orEmpty().map { it.toDomain() }
    }
}
