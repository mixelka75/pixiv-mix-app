package wtf.mxl.pixmix.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7A00),
    onPrimary = Color.Black,
    secondary = Color(0xFFFFB066),
    background = Color(0xFF0E0F12),
    surface = Color(0xFF15171C),
    onSurface = Color(0xFFE8E8EE),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE56A00),
    onPrimary = Color.White,
    secondary = Color(0xFFB04B00),
    background = Color(0xFFFCFAF7),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B1E),
)

@Composable
fun PixMixTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
