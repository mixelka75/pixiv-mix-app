package wtf.mxl.pixmix.shared.feature.illust

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import wtf.mxl.pixmix.shared.ui.ErrorState
import wtf.mxl.pixmix.shared.ui.ZoomableImage
import wtf.mxl.pixmix.shared.util.userMessage

@Composable
fun IllustViewerScreen(component: IllustViewerComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
            state.error != null -> ErrorState(
                message = "Не удалось загрузить страницы:\n${userMessageOf(state.error)}",
                modifier = Modifier.align(Alignment.Center),
                onRetry = component::retry,
            )
            else -> {
                val pagerState = rememberPagerState(
                    initialPage = component.initialIndex.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0)),
                    pageCount = { state.pages.size },
                )
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { i ->
                    // Pages can briefly become empty during retry — guard against ArrayIndexOOBE.
                    val page = state.pages.getOrNull(i) ?: return@HorizontalPager
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

        // Big tappable circular back button — original 24dp icon was nearly impossible
        // to hit on a phone with a black photo behind it.
        Surface(
            color = Color(0x88000000),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(44.dp),
        ) {
            IconButton(onClick = component::back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}

private fun userMessageOf(error: String?): String {
    if (error.isNullOrBlank()) return "Неизвестная ошибка"
    return runCatching { Throwable(error).userMessage() }.getOrDefault(error)
}
