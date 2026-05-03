package wtf.mxl.pixmix.shared.feature.illust

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import wtf.mxl.pixmix.shared.ui.ZoomableImage

@Composable
fun IllustViewerScreen(component: IllustViewerComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
            state.error != null -> Text(
                "Error: ${state.error}",
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                color = MaterialTheme.colorScheme.error,
            )
            else -> {
                val pagerState = rememberPagerState(
                    initialPage = component.initialIndex.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0)),
                    pageCount = { state.pages.size },
                )
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { i ->
                    val page = state.pages[i]
                    ZoomableImage(
                        url = page.originalUrl.ifBlank { page.regularUrl },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (state.pages.size > 1) {
                    Surface(
                        color = Color(0x88000000),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / ${state.pages.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = component::back,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}
