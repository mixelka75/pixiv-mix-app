# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Toolchain & build

Required env on this machine (no shell rc — set per-shell):
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH
```
`gradle.properties` already pins `org.gradle.java.home`, so the wrapper itself uses JDK 17 even without `JAVA_HOME`. `local.properties` carries `sdk.dir` and is git-ignored.

**Pin: AGP 8.5.2 is the max compatible with Kotlin 2.0.21** — bumping AGP will produce a Kotlin/AGP compatibility warning and may break KMP builds. Same goes for Coil 3 ↔ Ktor 3 versions in `gradle/libs.versions.toml` — keep them moving together.

### Common commands
```bash
./gradlew :composeApp:assembleDebug                                           # Android APK
./gradlew :composeApp:run                                                     # Desktop, run from sources
./gradlew :composeApp:packageDeb        # or packageAppImage                   # Desktop installers
./gradlew :composeApp:compileKotlinDesktop                                    # quick check desktop sources

adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am force-stop wtf.mxl.pixmix.debug
adb shell am start -n wtf.mxl.pixmix.debug/wtf.mxl.pixmix.MainActivity
adb logcat -s Ktor:V *:F                                                      # focused network logs
adb shell screencap -p /sdcard/p.png && adb pull /sdcard/p.png /tmp/          # debug screenshot
```

There are no tests yet. No `:test` target is wired.

## Architecture in one screen

Two Gradle modules: `composeApp` (entry points, manifests) and `shared` (everything else as KMP). Both target `androidTarget()` and `jvm("desktop")`.

```
shared/src/
├── commonMain/    domain, data layer, UI, screens, navigation, DI
├── androidMain/   actuals: SecureStorage(EncryptedSharedPreferences), WebViewHost(WebView), ZoomableImage(Telephoto), HttpEngine(OkHttp), Settings(SharedPreferences)
└── desktopMain/   actuals: SecureStorage(java.util.prefs), WebViewHost(opens system browser), ZoomableImage(transformGestures), HttpEngine(OkHttp), Settings(PreferencesSettings)

composeApp/src/
├── androidMain/   MainActivity + Application; constructs Decompose root via Koin
└── desktopMain/   Main.kt with singleWindowApplication; constructs root inside `application{}` so it lands on AWT EDT (Decompose enforces this)
```

### One Ktor client, manual cookies, optional rewrite

`shared/network/PixivHttpClient.kt` builds a single `HttpClient` with a custom `HttpSend` interceptor that:

1. Attaches cookies from `PersistentCookiesStorage` keyed against the canonical `https://www.pixiv.net/` URL — even if the request host is the user's proxy. Cookies are *always* stored as `.pixiv.net` regardless of the response host, so toggling the proxy never invalidates the session.
2. If `UserPrefs.proxy.enabled` and a `baseUrl` is set, rewrites `www.pixiv.net` → `<proxy>/pixiv` and `i.pximg.net` → `<proxy>/img`, then adds the `X-Pixmix-Token` header.

The Ktor `HttpCookies` plugin is intentionally **not** installed — that machinery would silently reject `Set-Cookie` whose `Domain` doesn't match the (rewritten) request host. Doing it manually is what makes the proxy switch transparent.

Coil 3 receives this same `HttpClient` via `KtorNetworkFetcherFactory`, so image requests automatically inherit the cookies, `Referer: https://www.pixiv.net/`, the proxy rewrite, and the disk cache.

For mutating AJAX endpoints (`/ajax/illusts/like`, `/ajax/illusts/bookmarks/add`, …), `CsrfTokenProvider` lazily fetches the pixiv home page, parses `<meta name="global-data" content='{...,"token":"..."}'>` (with a fallback to legacy `<input name="tt">`), caches for 25 min, and re-fetches on 4xx via the `withCsrfRetry` helper in `IllustRepository`.

### Coroutine cancellation discipline

All repository methods use `runCoroutineCatching` from `shared/util/RunCoroutineCatching.kt` instead of `runCatching`. Stdlib `runCatching` swallows `CancellationException`, which surfaces "StandaloneCoroutine was cancelled" as a user-visible error whenever a `loadJob.cancel()` runs (mode toggles, tab switches). When adding a new repo method, **always** use the helper (or hand-write the `try { ... } catch (c: CancellationException) { throw c }` pattern, e.g. as in `IllustRepository.withCsrfRetry`).

### Navigation: Decompose, not androidx.navigation

`RootComponent` owns a single `StackNavigation<Config>` with: `Login → Main → IllustDetail → IllustViewer`. The `Main` child is `MainTabsComponent`, which holds three child contexts (`home`, `search`, `ranking`, `settings`) all alive simultaneously and switches via `selected: StateFlow<Tab>`. **Don't** convert tabs to a `childStack` — keeping them alive is what preserves scroll/state when you switch tabs.

Decompose components play the role of ViewModels. They take a `ComponentContext`, expose `StateFlow`-based UI state, and manage their own `CoroutineScope` cancelled in `lifecycle.doOnDestroy`. The `RootComponent` constructor takes lambda factories for child components so deps can be injected explicitly per child without leaking Koin into screens.

### Adaptive UI

`MainTabsScreen.kt` switches between `BottomNavigation` (<720dp wide) and `NavigationRail` (≥720dp). `IllustGrid` uses `GridCells.Adaptive(140.dp)` so columns auto-scale 3 → 8. `IllustFeed` wraps its `LazyColumn` in `widthIn(max = 720.dp)` and centers — full-width feed cards on a wide monitor look bad.

### Image loading optimisation in feeds

`IllustFeed` computes a `hiResRange` from the visible window via `derivedStateOf`. Cards within `±3` of the viewport render `LayeredPixivImage(loadFullRes = true)` — small thumbnail underneath with master1200 on top crossfading in. Cards outside that range only render the small placeholder. A separate `LaunchedEffect` enqueues `imageLoader.enqueue(...)` for indices `+4..+8` to warm the disk cache silently, so further scrolling lands on already-cached files. `@Immutable` annotations on `IllustSummary`/`IllustDetail` let strong-skipping mode skip recomposition for unchanged cards.

`shared/util/PixivImageUrls.kt` rewrites the small square thumbnail URL we get from API responses into `i.pximg.net/c/600x1200_90/img-master/<path>/<id>_p<N>_master1200.jpg` (full-aspect, ~600px long side). Pixiv `_p0_square1200.jpg` → `_pN_master1200.jpg`, and the resize prefix `c/<size>/` is swapped — server-side resizing keeps bytes small. The `_pN_` swap is also how multi-page swipe in the feed works without needing a separate API call per page.

### Auth flow

WebView (`Android`) or system browser (`Desktop`) → `LoginComponent.onCookies` extracts `PHPSESSID` (format `<userId>_<random>`), seeds *every* captured cookie into `PersistentCookiesStorage` so the Ktor client inherits the full session (including `device_token` etc.), and stores `PHPSESSID` separately in `SessionStore` for "am I logged in?" checks. `RootComponent` observes `SessionStore.session` and `replaceAll` the stack on login/logout.

Google sign-in via the embedded WebView **does not work** — Google detects WebView and refuses since 2021. Users must use email+password directly on pixiv.net, or paste their PHPSESSID into the form (the form mode is the default).

## Self-hosted proxy (optional)

`deploy/nginx/pixmix.mxl.wtf.conf` is a self-contained reverse-proxy template for users who want to route pixiv traffic through their own VPS (faster than VPN, bypasses ISP blocks). The token in `set $pixmix_token "..."` must match what the user enters in the app's Settings → Proxy token field. **The VPS itself must be able to reach pixiv** — Russian VPS providers won't work without an upstream tunnel; use Hetzner Helsinki/Falkenstein or similar.

## Memory / context

User-facing memories under `~/.claude/projects/-home-mixel-projects-PixMix/memory/`. The plan that scoped the project lives at `~/.claude/plans/fluttering-wiggling-matsumoto.md`. Both are git-ignored.
