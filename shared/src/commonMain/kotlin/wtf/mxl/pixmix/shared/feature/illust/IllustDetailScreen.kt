package wtf.mxl.pixmix.shared.feature.illust

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import wtf.mxl.pixmix.shared.ui.ErrorState
import wtf.mxl.pixmix.shared.util.userMessage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.domain.model.IllustDetail
import wtf.mxl.pixmix.shared.domain.model.IllustPage
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.domain.model.XRestrict
import wtf.mxl.pixmix.shared.prefs.FeedLayout
import wtf.mxl.pixmix.shared.ui.FeedActions
import wtf.mxl.pixmix.shared.ui.FeedActionsHost
import wtf.mxl.pixmix.shared.ui.PixivImage

@Composable
fun IllustDetailScreen(component: IllustDetailComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    val layout by component.layout.collectAsState()
    val clipboard = LocalClipboardManager.current
    val snackbarHost = remember { SnackbarHostState() }
    val coScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.title ?: "Loading…", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = component::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.detail != null) {
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(
                                "https://www.pixiv.net/artworks/${state.detail!!.id}"
                            ))
                            coScope.launch { snackbarHost.showSnackbar("Ссылка скопирована") }
                        }) {
                            Icon(Icons.Filled.Link, contentDescription = "Скопировать ссылку")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        FeedActionsHost(controller = component.actions) { actions ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when {
                    state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.error != null -> ErrorState(
                        message = "Не удалось загрузить пост:\n${userMessageOf(state.error)}",
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = component::retry,
                    )
                    state.detail != null -> DetailContent(
                        detail = state.detail!!,
                        summary = component.toIllustSummary() ?: state.detail!!.toSummary(),
                        pages = state.pages,
                        related = state.related,
                        relatedLayout = layout,
                        loadingRelated = state.loadingRelated,
                        onPageClick = component::openViewer,
                        onRelatedClick = component::openIllust,
                        onTagClick = component::openTag,
                        actions = actions,
                    )
                }
            }
        }
    }
}

private fun userMessageOf(error: String?): String {
    if (error.isNullOrBlank()) return "Неизвестная ошибка"
    return runCatching { Throwable(error).userMessage() }.getOrDefault(error)
}

private fun IllustDetail.toSummary(): IllustSummary = IllustSummary(
    id = id,
    title = title,
    kind = kind,
    xRestrict = xRestrict,
    width = width,
    height = height,
    pageCount = pageCount,
    thumbnailUrl = previewUrl,
    tags = tags,
    author = author,
    isMasked = false,
)

@Composable
private fun DetailContent(
    detail: IllustDetail,
    summary: IllustSummary,
    pages: List<IllustPage>,
    related: List<IllustSummary>,
    relatedLayout: FeedLayout,
    loadingRelated: Boolean,
    onPageClick: (Int) -> Unit,
    onRelatedClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    actions: FeedActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        itemsIndexed(pages) { index, page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .clickable { onPageClick(index) },
            ) {
                // Use Modifier.aspectRatio so the image scales with the actual list width
                // rather than a hardcoded 360 (which made desktop images comically small).
                val ratio = (page.width.coerceAtLeast(1).toFloat() / page.height.coerceAtLeast(1))
                    .coerceIn(0.3f, 3f)
                PixivImage(
                    url = page.regularUrl,
                    contentDescription = detail.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(2.dp))
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(detail.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "by ${detail.author.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(12.dp))
                ActionRow(detail = detail, summary = summary, actions = actions)
                Spacer(Modifier.height(12.dp))
                StatsRow(detail)
                if (detail.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(detail.tags) { tag ->
                            AssistChip(
                                onClick = { onTagClick(tag) },
                                label = { Text("#$tag") },
                            )
                        }
                    }
                }
                if (detail.description.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(stripHtml(detail.description), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (related.isNotEmpty() || loadingRelated) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Related",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        when (relatedLayout) {
            FeedLayout.Grid -> {
                // Render as 3-column rows so we can stay inside the outer LazyColumn.
                related.chunked(3).forEach { row ->
                    item(key = "related-row-${row.firstOrNull()?.id}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            row.forEach { illust ->
                                Box(modifier = Modifier.weight(1f)) { RelatedCell(illust, onRelatedClick) }
                            }
                            repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) {} }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
            FeedLayout.Feed -> {
                items(related, key = { "related-feed-${it.id}" }) { illust ->
                    Spacer(Modifier.height(12.dp))
                    RelatedFeedCard(illust, onRelatedClick)
                }
            }
        }
    }
}

@Composable
private fun RelatedCell(illust: IllustSummary, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick(illust.id) },
    ) {
        PixivImage(url = illust.thumbnailUrl, contentDescription = illust.title, modifier = Modifier.fillMaxSize())
        if (illust.xRestrict == XRestrict.R18) MiniBadge("R-18", Modifier.align(Alignment.TopStart))
        if (illust.pageCount > 1) MiniBadge("${illust.pageCount}", Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun RelatedFeedCard(illust: IllustSummary, onClick: (String) -> Unit) {
    val ratio = (illust.width.toFloat() / illust.height.coerceAtLeast(1)).coerceIn(0.5f, 2.0f)
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick(illust.id) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .background(Color.Black),
        ) {
            PixivImage(
                url = illust.thumbnailUrl,
                contentDescription = illust.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (illust.xRestrict == XRestrict.R18) MiniBadge("R-18", Modifier.align(Alignment.TopStart).padding(8.dp))
            if (illust.pageCount > 1) MiniBadge("${illust.pageCount}", Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Text(
            illust.title.ifBlank { illust.author.name },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun MiniBadge(text: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier.padding(4.dp),
        color = Color(0xCC000000),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ActionRow(
    detail: IllustDetail,
    summary: IllustSummary,
    actions: FeedActions,
) {
    val liked = detail.isLiked || actions.isLiked(detail.id)
    val bookmarked = detail.isBookmarked || actions.isBookmarked(detail.id)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { actions.onLike(detail.id) }) {
            Icon(
                imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Like",
                tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = { actions.onBookmarkClick(summary) }) {
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = { actions.onDownloadClick(summary, null) }) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun StatsRow(detail: IllustDetail) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Stat("Views", detail.viewCount)
        Stat("Likes", detail.likeCount)
        Stat("Bookmarks", detail.bookmarkCount)
        Stat("Comments", detail.commentCount)
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

private fun stripHtml(s: String): String =
    s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#x27;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
