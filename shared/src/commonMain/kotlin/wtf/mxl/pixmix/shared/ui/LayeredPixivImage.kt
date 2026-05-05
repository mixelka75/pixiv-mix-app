package wtf.mxl.pixmix.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest

/**
 * Two-pass image: shows a small thumbnail immediately as a placeholder, then loads the
 * higher-resolution preview on top with a crossfade. Massively cuts perceived latency
 * for the feed view because the small variant is ~30 KB and lands in <100 ms.
 *
 * Renders a small status badge in the bottom-left while loading is in progress, so the
 * user can tell at a glance whether the visible blur is "still fetching" vs "this is
 * already the final quality". The badge fades away once the highest tier requested has
 * loaded (or after the small placeholder if hi-res wasn't requested).
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
    /** Hide the per-card loading badge (e.g. on tiny grid cells where it'd dominate). */
    showLoadingBadge: Boolean = true,
) {
    var placeholderReady by remember(placeholderUrl) { mutableStateOf(false) }
    var placeholderFailed by remember(placeholderUrl) { mutableStateOf(false) }
    var fullReady by remember(fullUrl) { mutableStateOf(false) }
    var fullFailed by remember(fullUrl) { mutableStateOf(false) }

    // Fire-and-forget prewarm: kick off enqueue requests for both URLs and *don't*
    // hold the Disposable. AsyncImage below cancels its own request via
    // AbortController when it leaves composition (visible in profile as
    // `io.ktor.client.fetch.abort_$external_fun` calls during scroll). An aborted
    // fetch never lands in the browser HTTP cache, so every re-entry into viewport
    // re-fetches from the network — observed as 14–18× requests per URL during
    // 19 sec of scroll. By enqueueing here and discarding the Disposable, Coil's
    // own scope keeps the request alive past the AsyncImage cancel; the response
    // populates Coil memory cache so the next AsyncImage mount serves locally.
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    LaunchedEffect(placeholderUrl) {
        if (placeholderUrl.isNotBlank()) {
            imageLoader.enqueue(ImageRequest.Builder(context).data(placeholderUrl).build())
        }
    }
    LaunchedEffect(fullUrl, loadFullRes) {
        if (loadFullRes && fullUrl.isNotBlank()) {
            imageLoader.enqueue(ImageRequest.Builder(context).data(fullUrl).build())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Once the full-res image lands we stop drawing the placeholder. The
        // placeholder is a square (toSmallSquareUrl) while the full image keeps the
        // native aspect — with ContentScale.Fit the square would otherwise stay
        // visible in the letterbox margins behind the (wider/taller) full image.
        if (!(loadFullRes && fullReady)) {
            AsyncImage(
                model = placeholderUrl,
                contentDescription = if (loadFullRes) null else contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> placeholderReady = true
                        is AsyncImagePainter.State.Error -> placeholderFailed = true
                        else -> Unit
                    }
                },
            )
        }
        if (loadFullRes) {
            AsyncImage(
                model = fullUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> fullReady = true
                        is AsyncImagePainter.State.Error -> fullFailed = true
                        else -> Unit
                    }
                },
            )
        }

        if (showLoadingBadge) {
            val status = when {
                loadFullRes && fullReady -> Status.Done
                loadFullRes && fullFailed && placeholderReady -> Status.HdFailed
                loadFullRes -> if (placeholderReady) Status.LoadingHd else Status.LoadingThumb
                placeholderReady -> Status.Done
                placeholderFailed -> Status.Failed
                else -> Status.LoadingThumb
            }
            AnimatedVisibility(
                visible = status != Status.Done,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            ) {
                StatusBadge(status)
            }
        }
    }
}

private enum class Status { LoadingThumb, LoadingHd, Done, HdFailed, Failed }

@Composable
private fun StatusBadge(status: Status) {
    val (label, spinning) = when (status) {
        Status.LoadingThumb -> "Загрузка…" to true
        Status.LoadingHd -> "HD…" to true
        Status.HdFailed -> "HD недоступно" to false
        Status.Failed -> "Не загрузилось" to false
        Status.Done -> "" to false
    }
    Surface(
        color = Color(0xCC000000),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (spinning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
