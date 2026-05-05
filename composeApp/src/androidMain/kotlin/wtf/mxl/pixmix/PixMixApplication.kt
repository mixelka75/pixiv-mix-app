package wtf.mxl.pixmix

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import wtf.mxl.pixmix.shared.di.androidPlatformModule
import wtf.mxl.pixmix.shared.di.sharedModule
import wtf.mxl.pixmix.shared.ui.buildImageLoader

class PixMixApplication : Application() {

    private val httpClient: HttpClient by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PixMixApplication)
            modules(androidPlatformModule, sharedModule)
        }

        SingletonImageLoader.setSafe { context: PlatformContext ->
            val cacheDir = cacheDir.resolve("image_cache").absolutePath
            buildImageLoader(
                context = context,
                httpClient = httpClient,
                diskCacheConfig = { builder ->
                    builder
                        .directory(cacheDir.toPath())
                        .maxSizeBytes(256L * 1024 * 1024)
                        .fileSystem(FileSystem.SYSTEM)
                        .build()
                },
            )
        }
    }
}
