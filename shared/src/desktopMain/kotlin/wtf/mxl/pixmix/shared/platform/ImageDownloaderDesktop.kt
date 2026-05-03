package wtf.mxl.pixmix.shared.platform

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import wtf.mxl.pixmix.shared.util.runCoroutineCatching
import java.io.File
import java.io.FileOutputStream

actual class ImageDownloader(
    private val httpClient: HttpClient,
) {
    actual suspend fun saveImage(url: String, suggestedFilename: String): Result<String> =
        runCoroutineCatching {
            withContext(Dispatchers.IO) {
                val response: HttpResponse = httpClient.get(url)
                if (!response.status.isSuccess()) {
                    error("download failed: HTTP ${response.status.value}")
                }
                val home = System.getProperty("user.home") ?: "."
                val dir = File(home, "Pictures/PixMix").apply { mkdirs() }
                val outFile = File(dir, suggestedFilename)
                FileOutputStream(outFile).use { out ->
                    val channel = response.bodyAsChannel()
                    val buf = ByteArray(64 * 1024)
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buf, 0, buf.size)
                        if (read <= 0) break
                        out.write(buf, 0, read)
                    }
                    out.flush()
                }
                outFile.absolutePath
            }
        }
}
