package wtf.mxl.pixmix.shared.data.repository

import wtf.mxl.pixmix.shared.data.api.PixivAjaxApi
import wtf.mxl.pixmix.shared.data.toDomain
import wtf.mxl.pixmix.shared.domain.model.IllustDetail
import wtf.mxl.pixmix.shared.domain.model.IllustPage
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.network.csrf.CsrfTokenProvider

class IllustRepository(
    private val api: PixivAjaxApi,
    private val csrf: CsrfTokenProvider,
) {

    suspend fun detail(id: String): Result<IllustDetail> = runCatching {
        val env = api.illust(id)
        if (env.error || env.body == null) error(env.message.ifBlank { "illust not found" })
        env.body.toDomain()
    }

    suspend fun pages(id: String): Result<List<IllustPage>> = runCatching {
        val env = api.illustPages(id)
        if (env.error || env.body == null) error(env.message.ifBlank { "no pages" })
        env.body.map { it.toDomain() }
    }

    suspend fun related(id: String, limit: Int = 18): Result<List<IllustSummary>> = runCatching {
        val env = api.recommendInit(id, limit = limit)
        if (env.error || env.body == null) error(env.message.ifBlank { "no related" })
        env.body.illusts.map { it.toDomain() }
    }

    suspend fun like(id: String): Result<Boolean> = withCsrfRetry {
        val env = api.like(id)
        if (env.error) error(env.message)
        env.body?.is_liked ?: true
    }

    suspend fun bookmark(id: String, private: Boolean = false): Result<String?> = withCsrfRetry {
        val env = api.bookmarkAdd(id, restrict = if (private) 1 else 0)
        if (env.error) error(env.message)
        env.body?.last_bookmark_id
    }

    suspend fun unbookmark(bookmarkId: String): Result<Unit> = withCsrfRetry {
        val env = api.bookmarkRemove(bookmarkId)
        if (env.error) error(env.message)
    }

    /** Pixiv may reject a stale CSRF token. We retry once with a fresh one. */
    private inline fun <T> withCsrfRetry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (first: Throwable) {
            csrf.invalidate()
            runCatching(block)
        }
    }
}
