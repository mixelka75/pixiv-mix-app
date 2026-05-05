package wtf.mxl.pixmix.shared.ui

/**
 * Per-platform tuning for image loading aggressiveness in the feeds. Web (wasmJs)
 * needs much tighter values:
 *  - no real disk cache; memory cache evicts under any pressure
 *  - browser HTTP cache observed not deduping repeat fetches in the profile (358
 *    network requests over 22 sec for ~30 unique URLs — the same image fetched
 *    8× in parallel within 1 sec)
 *  - decoded master1200 bitmaps are heavy and decoding eats the wasm main thread
 *
 * Android/Desktop have a disk cache and OkHttp dedup, so they can afford bigger
 * radii.
 */
expect val FEED_HI_RES_RADIUS: Int

/** When false, the +N..+M prefetch loop is skipped — useful on web where it
 *  multiplies request count and triggers re-decodes the cache cannot keep up with. */
expect val FEED_PREFETCH_ENABLED: Boolean
