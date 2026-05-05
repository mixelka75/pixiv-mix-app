package wtf.mxl.pixmix.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.domain.model.XRestrict
import wtf.mxl.pixmix.shared.util.toMasterPreviewUrl
import wtf.mxl.pixmix.shared.util.toSmallSquareUrl

/** Above this width the Feed layout switches to a packed masonry of tiles. */
val WIDE_FEED_BREAKPOINT = 720.dp

/** Minimum tile width — staggered grid picks as many columns as fit at this size. */
private val TILE_MIN_WIDTH = 220.dp

@Composable
fun IllustTileFeed(
    items: List<IllustSummary>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
    onEndReached: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    actions: FeedActions = FeedActions.Noop,
    hiResRadius: Int = 6,
) {
    if (onEndReached != null) {
        StaggeredInfiniteScrollEffect(state = state, threshold = 8, onEndReached = onEndReached)
    }

    val hiResRange by remember(state, hiResRadius) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) IntRange.EMPTY
            else (visible.first().index - hiResRadius)..(visible.last().index + hiResRadius)
        }
    }

    val context = LocalPlatformContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val prefetchRange by remember(state, hiResRadius) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) IntRange.EMPTY
            else (visible.last().index + hiResRadius + 1)..(visible.last().index + hiResRadius + 9)
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

    val sidePad = 12.dp
    val gutter = 6.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columnsCount = ((maxWidth - sidePad * 2 + gutter) / (TILE_MIN_WIDTH + gutter))
            .toInt().coerceAtLeast(1)
        // Width of a single tile column (in dp). Used to compute deterministic tile heights
        // so the staggered grid never leaves vertical gaps mid-column.
        val tileWidth = (maxWidth - sidePad * 2 - gutter * (columnsCount - 1)) / columnsCount

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columnsCount),
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(horizontal = sidePad, vertical = 0.dp),
            verticalItemSpacing = gutter,
            horizontalArrangement = Arrangement.spacedBy(gutter),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { index, illust ->
                Tile(
                    illust = illust,
                    tileWidth = tileWidth,
                    loadFullRes = index in hiResRange,
                    onClick = { onClick(illust.id) },
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun Tile(
    illust: IllustSummary,
    tileWidth: androidx.compose.ui.unit.Dp,
    loadFullRes: Boolean,
    onClick: () -> Unit,
    actions: FeedActions,
) {
    // Native source aspect ratio. If the API didn't include dims (width/height == 0),
    // default to a square so the tile doesn't degenerate to 0-height (which manifests as
    // big visual gaps because surrounding tiles try to fill around an invisible item).
    val ratio = if (illust.width <= 0 || illust.height <= 0) 1f
        else (illust.width.toFloat() / illust.height).coerceIn(0.3f, 3f)
    // Compute height in dp so the staggered grid measures items deterministically — using
    // Modifier.aspectRatio inside a staggered grid can produce mid-column gaps because the
    // grid pre-allocates space before the modifier resolves.
    val tileHeight = tileWidth / ratio
    val tileMod = Modifier
        .fillMaxWidth()
        .height(tileHeight)
        .background(MaterialTheme.colorScheme.background)
        .clickable(onClick = onClick)

    val pagerState = if (illust.pageCount > 1)
        rememberPagerState(pageCount = { illust.pageCount }) else null
    Box(modifier = tileMod) {
        if (pagerState == null) {
            LayeredPixivImage(
                placeholderUrl = illust.thumbnailUrl.toSmallSquareUrl(),
                fullUrl = illust.thumbnailUrl.toMasterPreviewUrl(),
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                loadFullRes = loadFullRes,
                showLoadingBadge = false,
            )
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { i ->
                LayeredPixivImage(
                    placeholderUrl = illust.thumbnailUrl.toSmallSquareUrl(i),
                    fullUrl = illust.thumbnailUrl.toMasterPreviewUrl(pageIndex = i),
                    contentDescription = illust.title,
                    contentScale = ContentScale.Crop,
                    loadFullRes = loadFullRes && i == pagerState.currentPage,
                    showLoadingBadge = false,
                )
            }
        }
        TileBadges(illust = illust, modifier = Modifier.fillMaxSize())
        if (pagerState != null) {
            Surface(
                color = Color(0xCC000000),
                modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "${pagerState.currentPage + 1}/${illust.pageCount}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        TileOverlayActions(
            illust = illust,
            actions = actions,
            currentPageIndex = pagerState?.currentPage,
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
        )
    }
}

@Composable
private fun TileBadges(illust: IllustSummary, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (illust.xRestrict == XRestrict.R18) TileBadge("R-18", Modifier.align(Alignment.TopStart))
        if (illust.xRestrict == XRestrict.R18G) TileBadge("R-18G", Modifier.align(Alignment.TopStart))
        if (illust.pageCount > 1) TileBadge("${illust.pageCount}", Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun TileBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(6.dp),
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
private fun StaggeredInfiniteScrollEffect(
    state: LazyStaggeredGridState,
    threshold: Int,
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
