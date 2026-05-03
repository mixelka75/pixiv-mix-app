package wtf.mxl.pixmix.shared.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import wtf.mxl.pixmix.shared.platform.SecureStorage
import wtf.mxl.pixmix.shared.platform.SecureStorageAndroid

val androidPlatformModule = module {
    single<SecureStorage> { SecureStorageAndroid(androidContext()) }
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("pixmix_prefs", Context.MODE_PRIVATE),
        )
    }
}
