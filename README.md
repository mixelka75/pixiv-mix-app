<p align="center">
  <img src="assets/icon.svg" width="128" alt="PixMix icon"/>
</p>
<h1 align="center">PixMix</h1>

<p align="center"><strong>English</strong> · <a href="README.ru.md">Русский</a></p>

Native Android + desktop client for pixiv built on top of the AJAX/web API. Kotlin Multiplatform + Compose Multiplatform. Optional self-hosted reverse proxy for users behind ISP-level pixiv blocks or wanting a fat upstream pipe.

---

## Project layout

```
.
├── composeApp/             Android entry point (MainActivity, Application, manifest)
├── shared/                 KMP module — domain, data, network, UI, screens
└── deploy/
    └── nginx/
        └── pixmix.mxl.wtf.conf    self-contained reverse-proxy template
```

## Build the app

Prereqs: JDK 17. For Android, also Android SDK with `platforms;android-34` and `build-tools;34.0.0`.

### Android

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

App ID (debug build): `wtf.mxl.pixmix.debug`. Launch:
```bash
adb shell am start -n wtf.mxl.pixmix.debug/wtf.mxl.pixmix.MainActivity
```

### Desktop (Linux / macOS / Windows)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew :composeApp:run                  # run from sources
./gradlew :composeApp:packageDeb           # build a .deb (Linux)
./gradlew :composeApp:packageAppImage      # AppImage runtime bundle
```

The desktop UI is adaptive: a `NavigationRail` shows on screens ≥720dp wide, the
feed auto-centers with a max width, and image grids scale columns to window width.

WebView-based login isn't available on desktop (would require shipping CEF or
JavaFX). Use the **PHPSESSID** field on the login screen — the app will offer to
open pixiv.net in your default browser so you can grab the cookie.

---

## Deploy the proxy

The reverse proxy is **optional**. The app talks to pixiv directly by default. Enable the proxy in **Settings → Use proxy** if you want to route traffic through your own server (faster than VPN, bypasses ISP-level pixiv blocks).

### Requirements
- A VPS where pixiv is reachable (Hetzner Helsinki/Falkenstein works; any RU VPS does **not** unless tunneled).
- Free ports 80/443 on the VPS (no other webserver).
- A DNS A-record pointing your subdomain (e.g. `pixmix.your-domain`) at the VPS IP. Verify with `dig pixmix.your-domain +short` before proceeding.

### One-shot setup (Debian/Ubuntu)

SSH into the VPS as root (or with `sudo`) and run:

```bash
# 1. install everything we need
apt update && apt install -y nginx certbot python3-certbot-nginx ufw curl ssl-cert

# 2. firewall
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

# 3. fetch the nginx template from this repo
curl -fsSL https://raw.githubusercontent.com/mixelka75/pixiv-mix-app/main/deploy/nginx/pixmix.mxl.wtf.conf \
  -o /etc/nginx/sites-available/pixmix.your-domain.conf

# 4. point it at YOUR domain (replace pixmix.mxl.wtf throughout)
sed -i 's|pixmix.mxl.wtf|pixmix.your-domain|g' /etc/nginx/sites-available/pixmix.your-domain.conf

# 5. generate a long shared token and bake it in
TOKEN=$(openssl rand -hex 32) && echo "TOKEN: $TOKEN"     # save this!
sed -i "s|REPLACE_ME_WITH_LONG_RANDOM_STRING|$TOKEN|" /etc/nginx/sites-available/pixmix.your-domain.conf

# 6. enable the site, sanity check, reload
ln -sf /etc/nginx/sites-available/pixmix.your-domain.conf /etc/nginx/sites-enabled/
systemctl enable --now nginx
nginx -t && systemctl reload nginx

# 7. obtain a real Let's Encrypt cert (rewrites the snakeoil paths in-place)
certbot --nginx -d pixmix.your-domain --redirect --non-interactive --agree-tos -m you@example.com

# 8. verify — without the token you should get 403, with it you should get 200/4xx from pixiv
curl -sI "https://pixmix.your-domain/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
curl -sI -H "X-Pixmix-Token: $TOKEN" "https://pixmix.your-domain/pixiv/ajax/discovery/artworks?mode=safe&limit=1"
```

### Connect the app

In the app: **Settings → Use proxy** (toggle on)
- **Proxy base URL**: `https://pixmix.your-domain`
- **Proxy token**: the value of `$TOKEN` from step 5

Pixiv session is preserved on toggle — no need to re-login.

### Caveats

- **Existing webserver on the VPS?** If something else (docker-proxy, apache, another nginx) holds 80/443, this template won't deploy as-is. Either dedicate a VPS to PixMix, or merge the two `location` blocks into your existing reverse proxy.
- **Pixiv blocked from the VPS?** `curl -I https://www.pixiv.net/` should return `200`. If you get `502` from your proxy, the VPS itself can't reach pixiv. Use a server in a region without RKN-style blocks (Hetzner Helsinki/Falkenstein, OVH, Vultr Tokyo, etc.).
- **Certificate auto-renewal**: certbot installs a systemd timer (`systemctl status certbot.timer`). Nothing else to do.

### Troubleshooting

```bash
nginx -t                                   # config syntax
journalctl -u nginx -n 50 --no-pager       # startup errors
tail -f /var/log/nginx/error.log           # runtime errors (auth, upstream, TLS)
adb logcat -s Ktor:V *:F                   # client-side network logs
```

---

## License

No license set yet — treat as all-rights-reserved.
