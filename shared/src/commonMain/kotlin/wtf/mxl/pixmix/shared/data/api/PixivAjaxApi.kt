package wtf.mxl.pixmix.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import wtf.mxl.pixmix.shared.network.csrf.CsrfTokenProvider
import wtf.mxl.pixmix.shared.data.api.dto.AjaxEnvelope
import wtf.mxl.pixmix.shared.data.api.dto.DiscoveryBody
import wtf.mxl.pixmix.shared.data.api.dto.IllustDetailDto
import wtf.mxl.pixmix.shared.data.api.dto.IllustPageDto
import wtf.mxl.pixmix.shared.data.api.dto.JsonRaw
import wtf.mxl.pixmix.shared.data.api.dto.RankingResponse
import wtf.mxl.pixmix.shared.data.api.dto.RecommendInitBody
import wtf.mxl.pixmix.shared.data.api.dto.SearchArtworksBody

enum class FeedMode(val wire: String) { Safe("safe"), All("all"), R18("r18") }

@Serializable
data class LikeRequest(val illust_id: String)

@Serializable
data class LikeBody(val is_liked: Boolean = false)

@Serializable
data class BookmarkAddRequest(
    val illust_id: String,
    val restrict: Int = 0,
    val comment: String = "",
    val tags: List<String> = emptyList(),
)

@Serializable
data class BookmarkAddBody(val last_bookmark_id: String? = null)

@Serializable
data class BookmarkRemoveRequest(val bookmark_id: String)

class PixivAjaxApi(
    private val client: HttpClient,
    private val csrf: CsrfTokenProvider,
) {

    private suspend fun csrfHeader(): String =
        csrf.get() ?: error("Could not obtain CSRF token. Are you logged in?")


    suspend fun discovery(mode: FeedMode, limit: Int = 60): AjaxEnvelope<DiscoveryBody> =
        client.get("https://www.pixiv.net/ajax/discovery/artworks") {
            parameter("mode", mode.wire)
            parameter("limit", limit)
            parameter("lang", "en")
        }.body()

    suspend fun illust(id: String): AjaxEnvelope<IllustDetailDto> =
        client.get("https://www.pixiv.net/ajax/illust/$id") {
            parameter("lang", "en")
        }.body()

    suspend fun illustPages(id: String): AjaxEnvelope<List<IllustPageDto>> =
        client.get("https://www.pixiv.net/ajax/illust/$id/pages") {
            parameter("lang", "en")
        }.body()

    suspend fun like(illustId: String): AjaxEnvelope<LikeBody> =
        client.post("https://www.pixiv.net/ajax/illusts/like") {
            contentType(ContentType.Application.Json)
            header("x-csrf-token", csrfHeader())
            setBody(LikeRequest(illust_id = illustId))
        }.body()

    suspend fun bookmarkAdd(
        illustId: String,
        restrict: Int = 0,
        comment: String = "",
        tags: List<String> = emptyList(),
    ): AjaxEnvelope<BookmarkAddBody> =
        client.post("https://www.pixiv.net/ajax/illusts/bookmarks/add") {
            contentType(ContentType.Application.Json)
            header("x-csrf-token", csrfHeader())
            setBody(BookmarkAddRequest(illust_id = illustId, restrict = restrict, comment = comment, tags = tags))
        }.body()

    suspend fun recommendInit(id: String, limit: Int = 18): AjaxEnvelope<RecommendInitBody> =
        client.get("https://www.pixiv.net/ajax/illust/$id/recommend/init") {
            parameter("limit", limit)
            parameter("lang", "en")
        }.body()

    suspend fun searchArtworks(
        keyword: String,
        page: Int = 1,
        order: String = "date_d",
        mode: FeedMode = FeedMode.All,
    ): AjaxEnvelope<SearchArtworksBody> =
        client.get("https://www.pixiv.net/ajax/search/artworks/$keyword") {
            parameter("word", keyword)
            parameter("order", order)
            parameter("mode", mode.wire)
            parameter("p", page)
            parameter("s_mode", "s_tag")
            parameter("type", "all")
            parameter("lang", "en")
        }.body()

    /**
     * @param mode one of: daily, weekly, monthly, rookie, original, daily_ai,
     *             daily_r18, weekly_r18, r18g, …
     * @param content one of: all, illust, manga, ugoira
     * @param date YYYYMMDD optional
     */
    suspend fun ranking(
        mode: String = "daily",
        content: String = "all",
        date: String? = null,
        page: Int = 1,
    ): RankingResponse =
        client.get("https://www.pixiv.net/ranking.php") {
            parameter("format", "json")
            parameter("mode", mode)
            if (content != "all") parameter("content", content)
            if (date != null) parameter("date", date)
            parameter("p", page)
        }.body()

    suspend fun bookmarkRemove(bookmarkId: String): AjaxEnvelope<JsonRaw> =
        client.post("https://www.pixiv.net/ajax/illusts/bookmarks/delete") {
            contentType(ContentType.Application.Json)
            header("x-csrf-token", csrfHeader())
            setBody(BookmarkRemoveRequest(bookmark_id = bookmarkId))
        }.body()
}
