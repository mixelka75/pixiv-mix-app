package wtf.mxl.pixmix.shared.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path.Companion.toPath

fun buildImageLoader(
    context: PlatformContext,
    httpClient: HttpClient,
    diskCachePath: String,
): ImageLoader = ImageLoader.Builder(context)
    .components { add(KtorNetworkFetcherFactory(httpClient = httpClient)) }
    .crossfade(200)
    .memoryCache {
        MemoryCache.Builder()
            .maxSizePercent(context, percent = 0.30)
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(diskCachePath.toPath())
            .maxSizeBytes(256L * 1024 * 1024)
            .fileSystem(FileSystem.SYSTEM)
            .build()
    }
    .build()
