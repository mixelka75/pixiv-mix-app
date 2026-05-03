package wtf.mxl.pixmix.shared.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import wtf.mxl.pixmix.shared.platform.SecureStorage
import wtf.mxl.pixmix.shared.platform.SecureStorageDesktop
import java.util.prefs.Preferences

val desktopPlatformModule = module {
    single<SecureStorage> { SecureStorageDesktop() }
    single<Settings> {
        PreferencesSettings(Preferences.userRoot().node("wtf/mxl/pixmix/prefs"))
    }
}
