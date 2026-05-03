<p align="center">
  <img src="assets/icon.svg" width="128" alt="PixMix icon"/>
</p>
<h1 align="center">PixMix</h1>

<p align="center"><strong>English</strong> · <a href="README.ru.md">Русский</a></p>

<p align="center">
A clean, native pixiv client for Android, Windows, macOS and Linux.<br/>
No ads, no web wrapper, no Electron — just your art feed.
</p>

<p align="center">
  <a href="https://github.com/mixelka75/pixiv-mix-app/releases/latest"><strong>↓ Download the latest release</strong></a>
</p>

---

## Features

- **Browse the way you want** — endless feed, grid, ranking, search by tag.
- **Multi-page works swipe inline** — no extra taps to see page 2/3/...
- **Adaptive layout** — phone-style nav on small windows, desktop sidebar on big ones, image grids that follow your window size.
- **Snappy image loading** — full-resolution images preload around the viewport, so scrolling stays smooth.
- **Likes & bookmarks** — works with your real pixiv account.
- **Optional self-hosted proxy** — for users behind ISPs that throttle or block pixiv. See [Self-hosted proxy](#self-hosted-proxy-optional) below.

---

## Install

Grab the file for your platform from the [latest release](https://github.com/mixelka75/pixiv-mix-app/releases/latest):

| Platform | File | What to do |
|---|---|---|
| **Android** | `PixMix-release.apk` | Download to your phone, tap to install. May need *Allow install from unknown sources*. |
| **Windows** | `PixMix-0.1.0.msi` | Double-click and follow the installer. |
| **macOS** | `PixMix-1.0.0.dmg` | Open, drag PixMix.app to Applications. First launch: right-click → Open (unsigned app warning). |
| **Linux (.deb)** | `pixmix_0.1.0-1_amd64.deb` | `sudo apt install ./pixmix_*.deb` — works on Debian/Ubuntu/Mint/Pop. |
| **Linux (AppImage)** | `PixMix-x86_64.AppImage` | `chmod +x PixMix-x86_64.AppImage && ./PixMix-x86_64.AppImage` — works on any distro. |

---

## First launch

You sign in with your **own pixiv account** — PixMix never sees your credentials, the login goes to pixiv directly.

- **Android**: tap *Sign in*, the in-app browser will open the pixiv login page. Sign in like usual.
- **Desktop**: pixiv's WebView login is blocked by Google, so use one of:
  - paste your `PHPSESSID` cookie (the login screen tells you how to grab it from pixiv.net), or
  - click *Open pixiv.net in browser*, sign in there, then come back and paste the cookie.

> **HiDPI on Linux (Hyprland/Sway)**: GUI looks tiny? Run with `PIXMIX_SCALE=2 ./PixMix-x86_64.AppImage`. GNOME and KDE auto-detect.

---

## Self-hosted proxy (optional)

If pixiv is slow or blocked at your ISP, you can route everything through your own VPS — usually faster than VPN, no client-side setup beyond pasting a URL and a token. The repo ships a one-shot nginx template.

See **[DEVELOPMENT.md → Self-hosted proxy](DEVELOPMENT.md#self-hosted-proxy)** for the deployment recipe.

In the app: **Settings → Use proxy** → enter your URL + token. Your pixiv session is preserved when toggling the proxy on or off.

---

## Build from source / contribute

PixMix is Kotlin Multiplatform (Android + JVM desktop) on top of Compose Multiplatform. Everything you need to set up the toolchain, run from sources, and ship a release is in **[DEVELOPMENT.md](DEVELOPMENT.md)**.

---

## License

No license set yet — treat as all-rights-reserved.
