package wtf.mxl.pixmix.shared.platform

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import wtf.mxl.pixmix.shared.util.runCoroutineCatching

/**
 * Web "download" — fetches the bytes via the proxy (same shared HttpClient that already
 * appends the pixiv proxy/Referer/cookies), wraps them in a Blob, and triggers a synthetic
 * `<a download>` click so the browser saves the file to the user's Downloads folder.
 */
actual class ImageDownloader(
    private val httpClient: HttpClient,
) {
    actual suspend fun saveImage(url: String, suggestedFilename: String): Result<String> =
        runCoroutineCatching {
            val response: HttpResponse = httpClient.get(url)
            if (!response.status.isSuccess()) {
                error("download failed: HTTP ${response.status.value}")
            }
            val bytes = response.readBytes()
            triggerBrowserDownload(bytes, suggestedFilename)
            "Downloads/$suggestedFilename"
        }
}

private fun triggerBrowserDownload(bytes: ByteArray, filename: String) {
    val array = JsArray<JsAny?>()
    val typed = bytes.toJsUint8Array()
    array[0] = typed
    val blob = Blob(array, BlobPropertyBag(type = "application/octet-stream"))
    val href = createObjectUrl(blob)
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = href
    anchor.download = filename
    anchor.style.display = "none"
    document.body?.appendChild(anchor)
    anchor.click()
    anchor.remove()
    revokeObjectUrl(href)
}

@JsFun("(arr) => URL.createObjectURL(arr)")
private external fun createObjectUrl(blob: Blob): String

@JsFun("(url) => URL.revokeObjectURL(url)")
private external fun revokeObjectUrl(url: String)

private fun ByteArray.toJsUint8Array(): JsAny {
    val len = size
    val out = newUint8Array(len)
    for (i in 0 until len) {
        setUint8(out, i, this[i].toInt())
    }
    return out
}

@JsFun("(len) => new Uint8Array(len)")
private external fun newUint8Array(len: Int): JsAny

@JsFun("(arr, i, v) => { arr[i] = v; }")
private external fun setUint8(arr: JsAny, i: Int, v: Int)
