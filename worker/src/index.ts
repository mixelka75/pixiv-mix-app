/**
 * pixmix-proxy — Cloudflare Worker that fronts pixiv.net and i.pximg.net for the
 * web/desktop/mobile builds of PixMix.
 *
 *   /pixiv/<path>  ->  https://www.pixiv.net/<path>
 *   /img/<path>    ->  https://i.pximg.net/<path>
 *
 * The browser cannot talk to pixiv directly because:
 *  - pixiv's /ajax/* endpoints don't send Access-Control-Allow-Origin.
 *  - i.pximg.net rejects requests without `Referer: https://www.pixiv.net/`.
 *  - Cookies are HttpOnly and pinned to pixiv.net, so cross-origin XHR can't
 *    forward PHPSESSID directly.
 *
 * This worker:
 *  - Adds CORS headers that allow the configured origin (e.g. pixiv.mxl.wtf).
 *  - Injects `Referer: https://www.pixiv.net/` on every upstream request.
 *  - Forwards the user's session cookies via the `X-Pixmix-Cookie` header
 *    (the browser can't store pixiv.net cookies cross-origin, so the client
 *    keeps PHPSESSID etc. in localStorage and re-sends them per request).
 *  - Surfaces upstream `Set-Cookie` headers as `X-Pixmix-Set-Cookie` so the
 *    client can persist newly-issued cookies.
 *  - Gates access with a shared secret (`PIXMIX_TOKEN`) so the worker isn't
 *    free for any internet stranger to abuse.
 */

export interface Env {
  /** Set with `wrangler secret put PIXMIX_TOKEN` — required to use the worker. */
  PIXMIX_TOKEN?: string;
  /** Single origin (e.g. https://pixiv.mxl.wtf) or "*". Set in wrangler.toml. */
  ALLOWED_ORIGIN: string;
}

const PIXIV_HOST = "www.pixiv.net";
const IMG_HOST = "i.pximg.net";

// Fallback list when the preflight didn't ask for anything specific. The actual
// allow-list reflects whatever headers the browser requested (Ktor's JS engine
// adds User-Agent and other unexpected ones, and chasing them one-by-one is a
// losing game) — we still gate the request itself with X-Pixmix-Token below.
const DEFAULT_ALLOWED_HEADERS = [
  "Content-Type",
  "Accept",
  "Accept-Language",
  "User-Agent",
  "X-Pixmix-Token",
  "X-Pixmix-Cookie",
  "X-CSRF-Token",
  "x-csrf-token",
].join(", ");

const EXPOSED_HEADERS = ["X-Pixmix-Set-Cookie"].join(", ");

/** Headers we generate ourselves on the upstream request — never copy from client. */
const STRIPPED_REQUEST_HEADERS = new Set([
  "host",
  "origin",
  "referer",
  "x-pixmix-token",
  "x-pixmix-cookie",
  "cf-connecting-ip",
  "cf-ipcountry",
  "cf-ray",
  "cf-visitor",
  "cf-worker",
  "x-forwarded-for",
  "x-forwarded-proto",
  "x-real-ip",
]);

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url);

    // CORS preflight — answered without a token check so the browser can
    // discover what's allowed before the actual request.
    if (req.method === "OPTIONS") {
      return cors(new Response(null, { status: 204 }), env, req);
    }

    // Health check (publicly readable so monitors don't need the token).
    if (url.pathname === "/" || url.pathname === "/health") {
      return cors(new Response("pixmix-proxy ok", { status: 200 }), env, req);
    }

    // Auth gate.
    const token = req.headers.get("X-Pixmix-Token");
    if (env.PIXMIX_TOKEN && token !== env.PIXMIX_TOKEN) {
      return cors(new Response("forbidden", { status: 403 }), env, req);
    }

    // Route.
    let upstream: URL;
    if (url.pathname.startsWith("/pixiv/")) {
      upstream = new URL(
        `https://${PIXIV_HOST}${url.pathname.slice("/pixiv".length)}${url.search}`,
      );
    } else if (url.pathname.startsWith("/img/")) {
      upstream = new URL(
        `https://${IMG_HOST}${url.pathname.slice("/img".length)}${url.search}`,
      );
    } else {
      return cors(new Response("not found", { status: 404 }), env, req);
    }

    // Build upstream request.
    const upstreamHeaders = new Headers();
    req.headers.forEach((v, k) => {
      if (STRIPPED_REQUEST_HEADERS.has(k.toLowerCase())) return;
      upstreamHeaders.set(k, v);
    });
    upstreamHeaders.set("Host", upstream.host);
    upstreamHeaders.set("Referer", `https://${PIXIV_HOST}/`);
    if (!upstreamHeaders.has("User-Agent")) {
      upstreamHeaders.set(
        "User-Agent",
        "Mozilla/5.0 (compatible; PixMix/0.2; +https://github.com/mixelka75/pixiv-mix-app)",
      );
    }

    // Cookie source priority:
    //   1. X-Pixmix-Cookie (web build — browser can't send pixiv.net cookies cross-origin)
    //   2. Cookie:        (native build — sends cookies directly, like an HTTP client)
    const passthroughCookie =
      req.headers.get("X-Pixmix-Cookie") ?? req.headers.get("Cookie");
    if (passthroughCookie) upstreamHeaders.set("Cookie", passthroughCookie);

    const init: RequestInit = {
      method: req.method,
      headers: upstreamHeaders,
      redirect: "manual",
    };
    if (req.method !== "GET" && req.method !== "HEAD") {
      init.body = req.body;
    }

    let upstreamRes: Response;
    try {
      upstreamRes = await fetch(upstream.toString(), init);
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      return cors(new Response(`upstream fetch failed: ${msg}`, { status: 502 }), env, req);
    }

    // Build response — strip Set-Cookie (browser would refuse cross-origin Set-Cookie
    // anyway) and surface it under a custom header so the client can persist sessions.
    const respHeaders = new Headers(upstreamRes.headers);
    const setCookie = upstreamRes.headers.get("Set-Cookie");
    respHeaders.delete("Set-Cookie");
    if (setCookie) respHeaders.set("X-Pixmix-Set-Cookie", setCookie);

    return cors(
      new Response(upstreamRes.body, {
        status: upstreamRes.status,
        statusText: upstreamRes.statusText,
        headers: respHeaders,
      }),
      env,
      req,
    );
  },
};

function cors(res: Response, env: Env, req: Request): Response {
  const h = new Headers(res.headers);
  const allow = env.ALLOWED_ORIGIN || "*";
  // Echo request origin if it matches the allowlist — safer than wildcard when
  // credentials are involved (which need an explicit origin, not "*").
  const reqOrigin = req.headers.get("Origin");
  if (allow === "*") {
    h.set("Access-Control-Allow-Origin", reqOrigin ?? "*");
  } else {
    h.set("Access-Control-Allow-Origin", allow);
  }
  h.set("Access-Control-Allow-Credentials", "true");
  h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  // Echo whatever the browser asked for in the preflight — works around Ktor's
  // JS engine setting headers (User-Agent etc.) the browser then treats as
  // "non-simple". Falls back to a baseline list for non-preflight responses.
  const requested = req.headers.get("Access-Control-Request-Headers");
  h.set("Access-Control-Allow-Headers", requested || DEFAULT_ALLOWED_HEADERS);
  h.set("Access-Control-Expose-Headers", EXPOSED_HEADERS);
  h.set("Access-Control-Max-Age", "86400");
  h.set("Vary", "Origin, Access-Control-Request-Headers");
  return new Response(res.body, {
    status: res.status,
    statusText: res.statusText,
    headers: h,
  });
}
