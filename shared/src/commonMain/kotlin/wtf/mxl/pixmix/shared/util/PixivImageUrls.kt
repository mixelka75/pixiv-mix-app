package wtf.mxl.pixmix.shared.util

/**
 * Discovery / search / ranking responses give us only a small SQUARE thumbnail like:
 *   https://i.pximg.net/c/250x250_90_a2_g5/img-master/img/2024/.../<id>_p0_square1200.jpg
 *
 * For a VK-style full-width feed we want pixiv's actual preview, which is hosted at:
 *   https://i.pximg.net/img-master/img/2024/.../<id>_p<N>_master1200.jpg
 *
 * Pixiv guarantees `_p0_master1200.jpg` exists for every artwork (longest side 1200),
 * with proper aspect ratio. We compute the URL by stripping the `c/<size>/` path segment
 * and rewriting the suffix.
 */
fun String.toMasterPreviewUrl(
    pageIndex: Int = 0,
    /** Pixiv resize prefix. Smaller = faster. `1200x1200_90` ~= master1200 with jpeg q=90 —
     *  long side capped at 1200px, which matches the largest preview pixiv serves. */
    resize: String = "1200x1200_90",
): String {
    if (isBlank()) return this
    var url = this

    // Replace any existing resize segment with our target one.
    url = if (Regex("/c/[^/]+/").containsMatchIn(url)) {
        url.replaceFirst(Regex("/c/[^/]+/"), "/c/$resize/")
    } else {
        url.replaceFirst("i.pximg.net/", "i.pximg.net/c/$resize/")
    }

    // Swap suffix: `_p0_square1200.jpg` → `_pN_master1200.jpg`
    url = url.replace(
        Regex("_p\\d+_(square|custom|master)\\d+\\.(jpg|png|webp)"),
        "_p${pageIndex}_master1200.\$2",
    )

    return url
}

/** Smaller still — used as quick placeholder while the main one loads. */
fun String.toSmallSquareUrl(pageIndex: Int = 0): String {
    if (isBlank()) return this
    var url = this
    url = if (Regex("/c/[^/]+/").containsMatchIn(url)) {
        url.replaceFirst(Regex("/c/[^/]+/"), "/c/360x360_70/")
    } else {
        url.replaceFirst("i.pximg.net/", "i.pximg.net/c/360x360_70/")
    }
    if (pageIndex > 0) {
        url = url.replace(Regex("_p\\d+_"), "_p${pageIndex}_")
    }
    return url
}
