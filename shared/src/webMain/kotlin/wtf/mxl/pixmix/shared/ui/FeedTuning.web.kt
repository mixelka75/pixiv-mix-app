package wtf.mxl.pixmix.shared.ui

/** Web (wasmJs) tuning: aggressive throttling. The browser HTTP cache is observed
 *  not deduping repeat fetches in profiles, and decoding master1200 bitmaps eats
 *  the wasm main thread. Keep only the immediately neighbouring card in HD and
 *  skip the +N..+M prefetch entirely. */
actual val FEED_HI_RES_RADIUS: Int = 1
actual val FEED_PREFETCH_ENABLED: Boolean = false
