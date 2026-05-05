package wtf.mxl.pixmix.shared.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import io.ktor.client.HttpClient

/**
 * Disk-cache config is platform-specific: Android/Desktop set up an okio-backed cache
 * under the user's app data dir; web has no real filesystem and skips the disk cache.
 * Pass `diskCacheConfig = null` to disable it (the default).
 */
fun buildImageLoader(
    context: PlatformContext,
    httpClient: HttpClient,
    diskCacheConfig: ((DiskCache.Builder) -> DiskCache)? = null,
): ImageLoader = ImageLoader.Builder(context)
    .components { add(KtorNetworkFetcherFactory(httpClient = httpClient)) }
    .crossfade(200)
    .memoryCache {
        MemoryCache.Builder()
            .maxSizePercent(context, percent = 0.30)
            .build()
    }
    .apply {
        if (diskCacheConfig != null) {
            diskCache { diskCacheConfig(DiskCache.Builder()) }
        }
    }
    .build()
