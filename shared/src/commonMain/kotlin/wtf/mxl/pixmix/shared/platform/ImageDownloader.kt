package wtf.mxl.pixmix.shared.platform

/**
 * Saves a remote pixiv image to a user-visible location (Pictures/PixMix on Android,
 * ~/Pictures/PixMix on Desktop). Uses the shared [io.ktor.client.HttpClient], so it
 * automatically inherits cookies, the Referer header, and the optional pixiv proxy
 * rewrite — without them, i.pximg.net replies 403.
 */
expect class ImageDownloader {
    /** @return Result with a human-readable destination string on success. */
    suspend fun saveImage(url: String, suggestedFilename: String): Result<String>
}
