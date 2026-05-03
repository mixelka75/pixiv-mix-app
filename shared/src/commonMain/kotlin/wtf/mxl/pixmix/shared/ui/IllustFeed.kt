package wtf.mxl.pixmix.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.domain.model.XRestrict
import wtf.mxl.pixmix.shared.util.toMasterPreviewUrl
import wtf.mxl.pixmix.shared.util.toSmallSquareUrl

@Composable
fun IllustFeed(
    items: List<IllustSummary>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
    onEndReached: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: LazyListState = rememberLazyListState(),
    /** Hi-res prefetch radius: cards within ±N of the visible window get the
     *  full pixiv master1200 image; the rest stay on the cheap 360px placeholder. */
    hiResRadius: Int = 3,
) {
    if (onEndReached != null) {
        InfiniteScrollEffect(state = state, threshold = 4, onEndReached = onEndReached)
    }

    val hiResRange by remember(state, hiResRadius) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) IntRange.EMPTY
            else (visible.first().index - hiResRadius)..(visible.last().index + hiResRadius)
        }
    }

    // Silently warm the disk cache for the next few off-screen items below the
    // viewport so they appear instantly when scrolled into the hi-res window.
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val prefetchRange by remember(state, hiResRadius) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) IntRange.EMPTY
            else (visible.last().index + hiResRadius + 1)..(visible.last().index + hiResRadius + 5)
        }
    }
    LaunchedEffect(prefetchRange, items) {
        prefetchRange.forEach { idx ->
            if (idx in items.indices) {
                val url = items[idx].thumbnailUrl.toMasterPreviewUrl()
                imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
        }
    }

    // On wide screens (desktop, tablets) cap the feed width so cards don't stretch
    // across half a monitor. Centered, max 720dp — close to phone-portrait reading width.
    Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { index, illust ->
                FeedCard(
                    illust = illust,
                    loadFullRes = index in hiResRange,
                    onClick = { onClick(illust.id) },
                )
            }
        }
    }
}

@Composable
private fun FeedCard(illust: IllustSummary, loadFullRes: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        FeedImageBlock(illust = illust, loadFullRes = loadFullRes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (illust.author.avatarUrl.isNotBlank()) {
                    PixivImage(
                        url = illust.author.avatarUrl,
                        contentDescription = illust.author.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    illust.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    illust.author.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FeedImageBlock(illust: IllustSummary, loadFullRes: Boolean) {
    val ratio = (illust.width.toFloat() / illust.height.coerceAtLeast(1))
        .coerceIn(0.6f, 1.6f)

    val imageMod = Modifier
        .fillMaxWidth()
        .aspectRatio(ratio)
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (illust.pageCount <= 1) {
        Box(modifier = imageMod) {
            LayeredPixivImage(
                placeholderUrl = illust.thumbnailUrl.toSmallSquareUrl(),
                fullUrl = illust.thumbnailUrl.toMasterPreviewUrl(),
                contentDescription = illust.title,
                loadFullRes = loadFullRes,
            )
            Badges(illust = illust, modifier = Modifier.fillMaxSize())
        }
    } else {
        // Multi-page: VK-style horizontal swipe between pages of the same post.
        // HorizontalPager itself only renders the current ± neighbouring pages,
        // so non-current pages won't even fetch a placeholder.
        val pagerState = rememberPagerState(pageCount = { illust.pageCount })
        Box(modifier = imageMod) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { i ->
                LayeredPixivImage(
                    placeholderUrl = illust.thumbnailUrl.toSmallSquareUrl(i),
                    fullUrl = illust.thumbnailUrl.toMasterPreviewUrl(pageIndex = i),
                    contentDescription = illust.title,
                    loadFullRes = loadFullRes && i == pagerState.currentPage,
                )
            }
            Badges(illust = illust, modifier = Modifier.fillMaxSize())
            Surface(
                color = Color(0xCC000000),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "${pagerState.currentPage + 1}/${illust.pageCount}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun Badges(illust: IllustSummary, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (illust.xRestrict == XRestrict.R18) Badge("R-18", Modifier.align(Alignment.TopStart))
        if (illust.xRestrict == XRestrict.R18G) Badge("R-18G", Modifier.align(Alignment.TopStart))
        if (illust.pageCount > 1) Badge("${illust.pageCount}", Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(8.dp),
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
fun InfiniteScrollEffect(
    state: LazyListState,
    threshold: Int = 4,
    onEndReached: () -> Unit,
) {
    val shouldLoadMore by remember(state) {
        derivedStateOf {
            val total = state.layoutInfo.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= total - 1 - threshold
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { if (it) onEndReached() }
    }
}
