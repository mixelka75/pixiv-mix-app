# pixmix-proxy

Cloudflare Worker that fronts `www.pixiv.net` and `i.pximg.net` so the PixMix web
build (and any other browser-resident client) can talk to pixiv despite CORS and
Referer restrictions.

This is a drop-in alternative to the nginx config under `deploy/nginx/` — works
the same way (path-rewrites + token gate + Referer injection) but runs on
Cloudflare's free tier (100 000 req/day) instead of your own VPS.

## Deploy

```bash
cd worker
npm install
npx wrangler login                                # one-time, opens browser
npx wrangler secret put PIXMIX_TOKEN              # paste the same token you'll
                                                  # set in the app's Settings
npx wrangler deploy
```

The worker lands at `https://pixmix-proxy.<your-account>.workers.dev`. Test:

```bash
curl https://pixmix-proxy.<your-account>.workers.dev/health
# → pixmix-proxy ok
```

## Configure CORS origin

Edit `wrangler.toml`:

```toml
[vars]
ALLOWED_ORIGIN = "https://pixiv.mxl.wtf"
```

Set to `"*"` to skip the origin check (not recommended — wildcard disables
credentials in the browser).

## Custom subdomain (optional)

If `mxl.wtf` is on Cloudflare you can route a friendly domain to the worker:

1. Cloudflare dashboard → Workers & Pages → `pixmix-proxy` → Triggers → Add
   Custom Domain → `api.pixiv.mxl.wtf`.
2. Wait ~30 s for the DNS record to propagate.
3. Use that URL as the proxy `baseUrl` in the app.

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
