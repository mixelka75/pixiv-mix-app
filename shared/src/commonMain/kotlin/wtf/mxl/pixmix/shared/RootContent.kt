package wtf.mxl.pixmix.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import wtf.mxl.pixmix.shared.feature.illust.IllustDetailScreen
import wtf.mxl.pixmix.shared.feature.illust.IllustViewerScreen
import wtf.mxl.pixmix.shared.feature.login.LoginScreen
import wtf.mxl.pixmix.shared.feature.main.MainTabsScreen
import wtf.mxl.pixmix.shared.navigation.RootComponent
import wtf.mxl.pixmix.shared.prefs.ThemeMode
import wtf.mxl.pixmix.shared.ui.PixMixTheme

@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    val mode by component.themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }
    PixMixTheme(darkTheme = dark) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Children(
                stack = component.stack,
                // Slide gives a sense of forward/back hierarchy when pushing into
                // IllustDetail/Viewer; fade smooths the cross-fade for tab-level swaps.
                animation = stackAnimation(slide() + fade()),
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Login -> LoginScreen(instance.component)
                    is RootComponent.Child.Main -> MainTabsScreen(instance.component)
                    is RootComponent.Child.IllustDetail -> IllustDetailScreen(instance.component)
                    is RootComponent.Child.IllustViewer -> IllustViewerScreen(instance.component)
                }
            }
        }
    }
}
