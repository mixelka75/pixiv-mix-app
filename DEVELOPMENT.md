# Development

<p><strong>English</strong> · <a href="DEVELOPMENT.ru.md">Русский</a></p>

Everything you need to build PixMix from sources, ship a release, run the optional self-hosted proxy, and find your way around the codebase.

---

## Toolchain

- **JDK 17** (Temurin or OpenJDK). Set `JAVA_HOME` per shell, or globally:
  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
  ```
  For a permanent local override use `~/.gradle/gradle.properties`:
  ```
  org.gradle.java.home=/usr/lib/jvm/java-17-openjdk
  ```
- **Android SDK** (only for Android builds): `platforms;android-34`, `build-tools;34.0.0`. Set:
  ```bash
  export ANDROID_HOME=$HOME/Android/Sdk
  export PATH=$ANDROID_HOME/platform-tools:$PATH
  ```
  `local.properties` carries `sdk.dir` and is git-ignored.
- **Pinned versions**: AGP 8.5.2 is the maximum compatible with Kotlin 2.0.21 — bumping AGP will produce a Kotlin/AGP compatibility warning and may break the KMP build. Coil 3 ↔ Ktor 3 versions in `gradle/libs.versions.toml` move together.

---

## Project layout

```
.
├── composeApp/                          entry points and platform manifests
│   ├── androidMain/                     MainActivity, Application, AndroidManifest
│   └── desktopMain/                     Main.kt with singleWindowApplication
├── shared/                              KMP module — almost all logic lives here
│   └── src/
│       ├── commonMain/                  domain, data, network, UI, navigation, DI
│       ├── androidMain/                 Android actuals (WebView, EncryptedSharedPreferences, Telephoto, OkHttp)
│       └── desktopMain/                 Desktop actuals (system browser, java.util.prefs, gesture-based zoom, OkHttp)
├── deploy/nginx/pixmix.mxl.wtf.conf     self-contained reverse-proxy template
└── .github/workflows/release.yml        CI/CD — builds + publishes a GitHub Release on tag push
```

Components:
- **Networking**: a single Ktor `HttpClient` with a custom `HttpSend` interceptor for cookie attachment + optional proxy URL rewriting. Coil 3 reuses the same client via `KtorNetworkFetcherFactory`, so images inherit cookies, the `Referer` header, the proxy, and the disk cache.
- **Navigation**: Decompose. `RootComponent` owns a `StackNavigation<Config>` with `Login → Main → IllustDetail → IllustViewer`. Tabs (`home`, `search`, `ranking`, `settings`) are kept alive simultaneously to preserve scroll/state on switching.
- **Auth**: WebView (Android) or system browser (Desktop) → cookies are seeded into a `PersistentCookiesStorage` keyed against `https://www.pixiv.net/` so that toggling the proxy never invalidates the session.
- **Adaptive UI**: `MainTabsScreen` switches between `BottomNavigation` (<720dp) and `NavigationRail` (≥720dp). Image grids use `GridCells.Adaptive(140.dp)`; the feed wraps a `LazyColumn` in `widthIn(max = 720.dp)` and centers.

---

## Build & run

```bash
# Android
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n wtf.mxl.pixmix.debug/wtf.mxl.pixmix.MainActivity

# Desktop — run from sources
./gradlew :composeApp:run

# Desktop — installers (host-OS only)
./gradlew :composeApp:packageDeb        # Debian/Ubuntu .deb (Linux)
./gradlew :composeApp:packageAppImage   # runtime bundle dir (Linux)
./gradlew :composeApp:packageMsi        # Windows .msi (Windows host)
./gradlew :composeApp:packageDmg        # macOS .dmg (macOS host)
```

The desktop `packageAppImage` task produces a directory, not a real `.AppImage`. The CI wraps it with `appimagetool` to make a single-file binary.

There are no tests yet.

---

## Releasing

Releases are fully automated via GitHub Actions (`.github/workflows/release.yml`).

1. Bump version where it matters: `composeApp/build.gradle.kts` (Android `versionName`/`versionCode`, Compose `packageVersion`).
2. Tag and push:
   ```bash
   git tag v0.1.1
   git push origin v0.1.1
   ```
3. The workflow runs four parallel jobs (Android, Linux, Windows, macOS), then a release job that uploads everything to GitHub Releases with auto-generated changelog.

### Android signing secrets

The `android` job signs the release APK if these repository secrets are set:

| Secret | Notes |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Output of `base64 -w0 keystore.jks`. |
| `ANDROID_KEY_ALIAS` | The key alias inside the keystore. |
| `ANDROID_KEYSTORE_PASSWORD` | Store password. |
| `ANDROID_KEY_PASSWORD` | Key password. **For PKCS12 keystores (default since JDK 9), this must equal the store password** — keytool ignores `-keypass` at creation time. |

If the secrets are missing the workflow falls back to building a debug APK so PRs from forks still build.

To generate a fresh keystore:
```bash
keytool -genkeypair -v -keystore ~/.android-keystores/pixmix-release.jks \
  -alias pixmix -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass <PWD> -keypass <PWD> -dname "CN=PixMix"

# upload as repo secrets
gh secret set ANDROID_KEYSTORE_BASE64 --body "$(base64 -w0 ~/.android-keystores/pixmix-release.jks)"
gh secret set ANDROID_KEY_ALIAS --body "pixmix"
gh secret set ANDROID_KEYSTORE_PASSWORD --body "<PWD>"
gh secret set ANDROID_KEY_PASSWORD --body "<PWD>"
```

> **Back the keystore up.** Lose it and you can't ship updates under the same `applicationId` to Google Play.

---

## Self-hosted proxy

The app talks to pixiv directly by default. The optional reverse proxy is for users behind ISP-level throttling/blocks of pixiv (faster than a VPN, single port, no client config beyond URL + token).

### Requirements

- A VPS where pixiv is reachable. Hetzner Helsinki/Falkenstein works; an RU VPS does **not** unless tunneled.
- Free ports 80/443 (no other webserver on the host).
- A DNS A-record pointing your subdomain (e.g. `pixmix.your-domain`) at the VPS IP. Verify with `dig pixmix.your-domain +short`.

### One-shot setup (Debian/Ubuntu)

SSH into the VPS as root (or with `sudo`):

```bash
# 1. install dependencies
apt update && apt install -y nginx certbot python3-certbot-nginx ufw curl ssl-cert

# 2. firewall
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

# 3. fetch the nginx template from this repo
curl -fsSL https://raw.githubusercontent.com/mixelka75/pixiv-mix-app/main/deploy/nginx/pixmix.mxl.wtf.conf \
  -o /etc/nginx/sites-available/pixmix.your-domain.conf

# 4. point it at YOUR domain
sed -i 's|pixmix.mxl.wtf|pixmix.your-domain|g' /etc/nginx/sites-available/pixmix.your-domain.conf

# 5. generate a long shared token and bake it in
TOKEN=$(openssl rand -hex 32) && echo "TOKEN: $TOKEN"     # save this!
sed -i "s|REPLACE_ME_WITH_LONG_RANDOM_STRING|$TOKEN|" /etc/nginx/sites-available/pixmix.your-domain.conf

# 6. enable, sanity-check, reload
ln -sf /etc/nginx/sites-available/pixmix.your-domain.conf /etc/nginx/sites-enabled/
systemctl enable --now nginx
nginx -t && systemctl reload nginx

# 7. obtain a real Let's Encrypt cert (rewrites the snakeoil paths in-place)
certbot --nginx -d pixmix.your-domain --redirect --non-interactive --agree-tos -m you@example.com

# 8. verify — without the token you should get 403, with it 200/4xx from pixiv
curl -sI "https://pixmix.your-domain/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
curl -sI -H "X-Pixmix-Token: $TOKEN" "https://pixmix.your-domain/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
```

In the app: **Settings → Use proxy** → enter `https://pixmix.your-domain` and your token.

### Caveats

- **Existing webserver on the VPS?** If something else holds 80/443, this template won't deploy as-is. Either dedicate a VPS to PixMix, or merge the two `location` blocks into your existing reverse proxy.
- **Pixiv blocked from the VPS?** `curl -I https://www.pixiv.net/` should return `200`. If you get `502` from your proxy, the VPS itself can't reach pixiv. Use a server in a region without RKN-style blocks (Hetzner Helsinki/Falkenstein, OVH, Vultr Tokyo).
- **Cert auto-renewal**: certbot installs a systemd timer (`systemctl status certbot.timer`).

### Troubleshooting

```bash
nginx -t                                   # config syntax
journalctl -u nginx -n 50 --no-pager       # startup errors
tail -f /var/log/nginx/error.log           # runtime errors (auth, upstream, TLS)
adb logcat -s Ktor:V *:F                   # client-side network logs
```

---

## Useful daily commands

```bash
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am force-stop wtf.mxl.pixmix.debug
adb shell am start -n wtf.mxl.pixmix.debug/wtf.mxl.pixmix.MainActivity
adb logcat -s Ktor:V *:F                   # focused network logs
adb shell screencap -p /sdcard/p.png && adb pull /sdcard/p.png /tmp/

./gradlew :composeApp:compileKotlinDesktop # quick desktop type-check
```
