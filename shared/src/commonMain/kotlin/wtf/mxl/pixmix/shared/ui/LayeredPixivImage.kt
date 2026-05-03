package wtf.mxl.pixmix.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Two-pass image: shows a small thumbnail immediately as a placeholder, then loads the
 * higher-resolution preview on top with a crossfade. Massively cuts perceived latency
 * for the feed view because the small variant is ~30 KB and lands in <100 ms.
 */
@Composable
fun LayeredPixivImage(
    placeholderUrl: String,
    fullUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    /** When false, the heavier full-res request is skipped — only the small
     *  placeholder is rendered. Used by feed views to avoid eagerly fetching
     *  hi-res for cards far from the viewport. */
    loadFullRes: Boolean = true,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = placeholderUrl,
            contentDescription = if (loadFullRes) null else contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )
        if (loadFullRes) {
            AsyncImage(
                model = fullUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}
