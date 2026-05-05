# pixmix-proxy

Cloudflare Worker that fronts `www.pixiv.net` and `i.pximg.net` so the PixMix web
build (and any other browser-resident client) can talk to pixiv despite CORS and
Referer restrictions.

This is a drop-in alternative to the nginx config under `deploy/nginx/` — works
the same way (path-rewrites + token gate + Referer injection) but runs on
Cloudflare's free tier (100 000 req/day) instead of your own VPS.

## Setup (anyone forking — repo doesn't ship a real `wrangler.toml`)

```bash
cd worker
cp wrangler.example.toml wrangler.toml         # personalise this in your fork —
                                               # `wrangler.toml` is gitignored
npm install
npx wrangler login                             # one-time, opens browser
npx wrangler secret put PIXMIX_TOKEN           # any random string; you'll paste
                                               # the same value into the app's
                                               # Settings → Proxy → Token
npx wrangler deploy
```

After deploy you'll see a URL like `https://pixmix-proxy.<your-account>.workers.dev`.
Quick check:

```bash
curl https://pixmix-proxy.<your-account>.workers.dev/health
# → pixmix-proxy ok
```

## Personalising `wrangler.toml`

Two things in the example file you might want to change before deploying:

```toml
# routes = [
#     { pattern = "api.pixiv.example.com", custom_domain = true },
# ]

[vars]
ALLOWED_ORIGIN = "*"
```

* **Custom domain** — uncomment the `routes` block and set the pattern to a
  hostname on a Cloudflare-managed zone in the same account. wrangler creates
  the DNS record + SSL automatically; the friendly URL is up within a minute.
* **CORS origin** — set this to the URL where your web client will be hosted
  (e.g. `https://pixiv.example.com`). Wildcard `*` works for personal/native
  use, but the browser refuses to send credentials with a wildcard origin, so
  the web build can't authenticate against `*`.

## Routes

| Path | Upstream |
|---|---|
| `/pixiv/<path>` | `https://www.pixiv.net/<path>` |
| `/img/<path>`   | `https://i.pximg.net/<path>` |
| `/health`       | health check, no auth |

## Required client headers

| Header | Purpose |
|---|---|
| `X-Pixmix-Token` | Shared secret (matches `PIXMIX_TOKEN` set above). |
| `X-Pixmix-Cookie` | Forwarded as `Cookie:` to pixiv (browser can't send pixiv.net cookies cross-origin). The web build keeps `PHPSESSID` etc. in `localStorage` and re-sends every request. |

## Response headers

| Header | Purpose |
|---|---|
| `X-Pixmix-Set-Cookie` | Mirrors upstream `Set-Cookie`. The web client should parse this and update its cookie store. |

## Tail logs

```bash
npx wrangler tail
```
