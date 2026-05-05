package wtf.mxl.pixmix.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun ZoomableImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
) {
    var scale by remember(url) { mutableStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            // Mouse wheel zoom — without this, desktop users with no touchpad pinch
            // can't zoom at all and the entire ZoomableImage degenerates to a still.
            .onPointerEvent(PointerEventType.Scroll) { e ->
                val deltaY = e.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (deltaY != 0f) {
                    val factor = if (deltaY < 0f) 1.15f else 1f / 1.15f
                    scale = (scale * factor).coerceIn(0.5f, 8f)
                }
            }
            // Double-click to toggle 1× ↔ 2×, reset pan.
            .pointerInput(url) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1.01f) 1f else 2f
                    offset = Offset.Zero
                })
            }
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 8f)
                    offset += pan
                }
            },
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
            contentScale = ContentScale.Fit,
        )
    }
}
