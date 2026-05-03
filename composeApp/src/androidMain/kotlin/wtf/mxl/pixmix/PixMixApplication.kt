package wtf.mxl.pixmix

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import io.ktor.client.HttpClient
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
            buildImageLoader(
                context = context,
                httpClient = httpClient,
                diskCachePath = cacheDir.resolve("image_cache").absolutePath,
            )
        }
    }
}
