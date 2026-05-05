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
    /** Fraction of available memory the in-memory bitmap cache may occupy on platforms
     *  where Coil can introspect it (JVM/Android). Ignored when [memoryCacheMaxSizeBytes]
     *  is non-null. */
    memoryCacheMaxSizePercent: Double = 0.30,
    /** Hard byte cap on the in-memory bitmap cache. Use this on web (wasmJs) where
     *  `maxSizePercent` cannot read process memory and may resolve to a tiny default,
     *  causing thrashing — same image is decoded/fetched repeatedly during scroll
     *  (observed in Firefox profile: identical URLs loaded 14–18× in 19 sec). */
    memoryCacheMaxSizeBytes: Long? = null,
): ImageLoader = ImageLoader.Builder(context)
    .components { add(KtorNetworkFetcherFactory(httpClient = httpClient)) }
    .crossfade(200)
    .memoryCache {
        val b = MemoryCache.Builder()
        if (memoryCacheMaxSizeBytes != null) {
            b.maxSizeBytes(memoryCacheMaxSizeBytes)
        } else {
            b.maxSizePercent(context, percent = memoryCacheMaxSizePercent)
        }
        b.build()
    }
    .apply {
        if (diskCacheConfig != null) {
            diskCache { diskCacheConfig(DiskCache.Builder()) }
        }
    }
    .build()
