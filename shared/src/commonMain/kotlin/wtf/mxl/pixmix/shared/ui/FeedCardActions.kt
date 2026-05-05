package wtf.mxl.pixmix.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.domain.model.IllustSummary

/**
 * Callbacks the feed/tile rows need from their host screen. Bundled because all three
 * feed entry points (Home, Search, Ranking) render the same set of buttons identically.
 */
@Stable
data class FeedActions(
    val likedIds: Set<String>,
    val bookmarkedIds: Set<String>,
    val onLike: (String) -> Unit,
    val onBookmarkClick: (IllustSummary) -> Unit,
    val onDownloadClick: (IllustSummary, currentPageIndex: Int?) -> Unit,
) {
    fun isLiked(id: String) = id in likedIds
    fun isBookmarked(id: String) = id in bookmarkedIds

    companion object {
        val Noop = FeedActions(
            likedIds = emptySet(),
            bookmarkedIds = emptySet(),
            onLike = {},
            onBookmarkClick = {},
            onDownloadClick = { _, _ -> },
        )
    }
}

/**
 * Compact icon row used inside the metadata strip of a feed card.
 */
@Composable
fun FeedCardActions(
    illust: IllustSummary,
    actions: FeedActions,
    currentPageIndex: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ActionIcon(
            icon = if (actions.isLiked(illust.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            tinted = actions.isLiked(illust.id),
            description = "Like",
            onClick = { actions.onLike(illust.id) },
        )
        ActionIcon(
            icon = if (actions.isBookmarked(illust.id)) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            tinted = actions.isBookmarked(illust.id),
            description = "Bookmark",
            onClick = { actions.onBookmarkClick(illust) },
        )
        ActionIcon(
            icon = Icons.Filled.Download,
            tinted = false,
            description = "Download",
            onClick = { actions.onDownloadClick(illust, currentPageIndex) },
        )
    }
}

/**
 * Variant for tile (grid) overlay — translucent dark background so icons stay legible
 * over arbitrary illust colors.
 */
@Composable
fun TileOverlayActions(
    illust: IllustSummary,
    actions: FeedActions,
    currentPageIndex: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x99000000))
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        OverlayIcon(
            icon = if (actions.isLiked(illust.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            tinted = actions.isLiked(illust.id),
            description = "Like",
            onClick = { actions.onLike(illust.id) },
        )
        OverlayIcon(
            icon = if (actions.isBookmarked(illust.id)) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            tinted = actions.isBookmarked(illust.id),
            description = "Bookmark",
            onClick = { actions.onBookmarkClick(illust) },
        )
        OverlayIcon(
            icon = Icons.Filled.Download,
            tinted = false,
            description = "Download",
            onClick = { actions.onDownloadClick(illust, currentPageIndex) },
        )
    }
}

/**
 * Single-icon overlay used in dense layouts (grid cells) where only the bookmark
 * action makes sense — the cell is too small for a full action row.
 */
@Composable
fun BookmarkOverlayButton(
    illust: IllustSummary,
    actions: FeedActions,
    modifier: Modifier = Modifier,
) {
    val bookmarked = actions.isBookmarked(illust.id)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(32.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0x99000000)),
    ) {
        IconButton(
            onClick = { actions.onBookmarkClick(illust) },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (bookmarked) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    tinted: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (tinted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun OverlayIcon(
    icon: ImageVector,
    tinted: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (tinted) MaterialTheme.colorScheme.primary else Color.White,
        )
    }
}
