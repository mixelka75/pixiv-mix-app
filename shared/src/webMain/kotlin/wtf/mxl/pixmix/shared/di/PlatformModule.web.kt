package wtf.mxl.pixmix.shared.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import org.koin.dsl.module
import wtf.mxl.pixmix.shared.platform.ImageDownloader
import wtf.mxl.pixmix.shared.platform.SecureStorage
import wtf.mxl.pixmix.shared.platform.SecureStorageWeb

val webPlatformModule = module {
    single<SecureStorage> { SecureStorageWeb() }
    single<Settings> { StorageSettings() }
    single { ImageDownloader(httpClient = get()) }
}
