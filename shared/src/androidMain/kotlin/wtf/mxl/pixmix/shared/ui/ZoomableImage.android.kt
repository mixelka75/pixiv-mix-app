package wtf.mxl.pixmix.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.request.ImageRequest
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ZoomableImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
) {
    val context = LocalContext.current
    ZoomableAsyncImage(
        model = ImageRequest.Builder(context).data(url).build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
