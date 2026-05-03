package wtf.mxl.pixmix.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.domain.model.XRestrict

@Composable
fun IllustGrid(
    items: List<IllustSummary>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    onClick: (String) -> Unit,
    onEndReached: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(2.dp),
    state: LazyGridState = rememberLazyGridState(),
) {
    if (onEndReached != null) {
        GridInfiniteScrollEffect(state = state, threshold = 6, onEndReached = onEndReached)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(items, key = { it.id }) { illust ->
            IllustCell(illust = illust, onClick = { onClick(illust.id) })
        }
    }
}

@Composable
private fun IllustCell(illust: IllustSummary, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        PixivImage(url = illust.thumbnailUrl, contentDescription = illust.title, modifier = Modifier.fillMaxSize())
        if (illust.xRestrict == XRestrict.R18) Badge("R-18", Modifier.align(Alignment.TopStart))
        if (illust.xRestrict == XRestrict.R18G) Badge("R-18G", Modifier.align(Alignment.TopStart))
        if (illust.pageCount > 1) Badge("${illust.pageCount}", Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    Surface(
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
private fun GridInfiniteScrollEffect(
    state: LazyGridState,
    threshold: Int = 6,
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
