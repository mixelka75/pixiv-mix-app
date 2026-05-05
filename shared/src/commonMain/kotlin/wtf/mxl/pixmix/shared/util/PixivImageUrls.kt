package wtf.mxl.pixmix.shared.util

/**
 * Discovery / search / ranking responses give us thumbnails in two flavours:
 *   • https://i.pximg.net/c/<size>/img-master/img/<date>/<id>_p0_square1200.jpg
 *   • https://i.pximg.net/c/<size>/custom-thumb/img/<date>/<id>_p0_custom1200.jpg
 *
 * For a VK-style full-width feed we want pixiv's actual master preview, which lives at:
 *   https://i.pximg.net/img-master/img/<date>/<id>_p<N>_master1200.jpg
 *
 * Note: `_master1200.jpg` only exists under `img-master/` — there's no
 * `custom-thumb/.../_master1200.jpg`, so we have to swap *both* the path segment
 * and the suffix when the input is the custom-thumb variant. Doing only the suffix
 * swap (the original implementation) produced 404s for ~35% of feed cards on
 * recent uploads, which is what triggered the visible blur in the feed.
 *
 * We also drop any `c/<size>/` resize prefix so the request hits the native
 * master1200 file — going through pixiv's resize service often returns visibly
 * degraded JPEGs.
 */
fun String.toMasterPreviewUrl(pageIndex: Int = 0): String {
    if (isBlank()) return this
    var url = this
    // Drop the resize prefix.
    url = url.replaceFirst(Regex("/c/[^/]+/"), "/")
    // master1200 only exists under img-master/ — relocate from custom-thumb/.
    url = url.replaceFirst("/custom-thumb/", "/img-master/")
    // Swap suffix: `_p?_(square|custom|master)<size>.<ext>` → `_pN_master1200.<ext>`
    url = url.replace(
        Regex("_p\\d+_(square|custom|master)\\d+\\.(jpg|png|webp)"),
        "_p${pageIndex}_master1200.\$2",
    )
    return url
}

/** Smaller still — used as quick placeholder while the main one loads.
 *  Both `img-master/.../_square1200` and `custom-thumb/.../_custom1200` have working
 *  `c/360x360_70/` resized variants, so no path-segment relocation is needed here. */
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
