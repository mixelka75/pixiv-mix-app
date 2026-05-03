package wtf.mxl.pixmix.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import wtf.mxl.pixmix.shared.feature.illust.IllustDetailScreen
import wtf.mxl.pixmix.shared.feature.illust.IllustViewerScreen
import wtf.mxl.pixmix.shared.feature.login.LoginScreen
import wtf.mxl.pixmix.shared.feature.main.MainTabsScreen
import wtf.mxl.pixmix.shared.navigation.RootComponent
import wtf.mxl.pixmix.shared.ui.PixMixTheme

@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    PixMixTheme {
        Children(
            stack = component.stack,
            modifier = modifier,
            animation = stackAnimation(fade()),
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
