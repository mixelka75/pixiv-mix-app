package wtf.mxl.pixmix.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pan + pinch-zoomable image. On Android backed by Telephoto's ZoomableAsyncImage,
 * which uses the singleton Coil ImageLoader (so it inherits the pixiv-aware Ktor client).
 */
@Composable
expect fun ZoomableImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
)
