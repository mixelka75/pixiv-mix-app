package wtf.mxl.pixmix.shared.platform

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import wtf.mxl.pixmix.shared.util.runCoroutineCatching
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

actual class ImageDownloader(
    private val context: Context,
    private val httpClient: HttpClient,
) {
    actual suspend fun saveImage(url: String, suggestedFilename: String): Result<String> =
        runCoroutineCatching {
            withContext(Dispatchers.IO) {
                val response: HttpResponse = httpClient.get(url)
                if (!response.status.isSuccess()) {
                    error("download failed: HTTP ${response.status.value}")
                }
                val mime = mimeFor(suggestedFilename)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaMediaStore(suggestedFilename, mime, response.bodyAsChannel())
                } else {
                    saveLegacy(suggestedFilename, response.bodyAsChannel())
                }
            }
        }

    private suspend fun saveViaMediaStore(name: String, mime: String, body: ByteReadChannel): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PixMix")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)?.use { copy(body, it) }
                ?: error("could not open output stream for $uri")
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
        val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        resolver.update(uri, done, null, null)
        return uri.toString()
    }

    private suspend fun saveLegacy(name: String, body: ByteReadChannel): String {
        @Suppress("DEPRECATION")
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(baseDir, "PixMix").apply { mkdirs() }
        val outFile = File(dir, name)
        FileOutputStream(outFile).use { copy(body, it) }
        // Make the new file visible in the gallery.
        android.media.MediaScannerConnection.scanFile(
            context, arrayOf(outFile.absolutePath), null, null,
        )
        return outFile.absolutePath
    }

    private suspend fun copy(channel: ByteReadChannel, out: OutputStream) {
        val buf = ByteArray(64 * 1024)
        while (!channel.isClosedForRead) {
            val read = channel.readAvailable(buf, 0, buf.size)
            if (read <= 0) break
            out.write(buf, 0, read)
        }
        out.flush()
    }

    private fun mimeFor(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/*"
        }
    }
}

