# Prince Ali Rescue Build 82 Review Verdict — 2026-10-01 19:15 EDT

**Verdict: PASS WITH FINDINGS**

- Build: 82 | Patch: 775 | Commit: 2155febe ("use bounded URLConnection quote request after native HTTP client loopback failure")
- Scope: read-only review (Alex owns implementation/releases). No source edited, nothing compiled, nothing uploaded.

## Custody (byte-level, verified this run)

- `patches/patch-775.zip` (blob fac326ba…): 246 entries, root `net/`, in-zip `version.txt` = `775`.
- `patches/princealirescue-82.jar` (blob 778e1ae8…): sha256 `17fa8b7dae358beb57e22c7162fc8a94754786735a7ca75361b83693e71caca7` — EXACT match to `patch-775.hot.json`.
- Script classes zip↔jar: 28/28 byte-identical. Plugin/Config classes as in B81 (Plugin javap-identical, Config constant `8026216c…`).
- `BUILD_NUMBER` = 82 via `javap -constants`; RUNNING_BUILD log, `getBuildNumber()`, status.properties all 82 — banner honest.
- Commit 2155febe single-purpose (9 files, same layout as B81).

## Semantic delta (B81 → B82; 89 changed CFR lines)

1. `fetchPrice()` rewritten from `java.net.http.HttpClient` to `java.net.HttpURLConnection`: connect timeout 4 s, read timeout 5 s, body capped at 64 KB (`readNBytes`), proper `try/finally` with `disconnect()`, JSON parsed with gson exactly as before (high/highTime presence, freshness ≤30 min, positive). This is the fix for the B81 quote-path failure.
2. Startup recovery (status.properties load path): if held on `NATIVE_GE_BUY` with error containing `Wiki quote unavailable:` + `Unable to establish loopback connection` and checkpoint prefix `GE2|333|4|1000|HOLD|0|-1|-1|-1|`, the checkpoint and buyer are discarded and the flow restarts at `RETRY_QUOTE_WITH_URL_CONNECTION` (cosmetic phase label; the real recovery is the state reset — `nativeGeBuy` then constructs a fresh buyer and re-quotes).
3. Removed the `PromoProbe` nested class (added in B81, never referenced by the live path — dead-code cleanup; no functional impact).

## API / thread-safety verification

- `HttpURLConnection` is `java.base` — present in the portable JRE (unlike `java.net.http`). Connect/read timeouts are bounded; no unbounded network wait on the tick thread. `disconnect()` in `finally` — no connection leak.
- Threading unchanged from B81 (snapshot on client thread, actions via `Rs2*`).

## FINDINGS

- **[carried] MEDIUM — silent idle livelock in `nativeGeBuy` COMPLETE branch** (introduced B81, unchanged here): `frame.count(333) < sourceGoal` → bare `return`, no HOLD/log/state change. Low reachability, total silence on hit.
- **[carried] MEDIUMs (all five):** poison soft-lock (B77/B79); stranded probe dumps; bronze pickaxe 1265 never withdrawn; B70 empty-`getQuestion()` furnace HOLD; B74 reloaded-walk arrival gate. None touched by B82.
- **LOW [NEW] — loopback recovery matcher is narrow.** It matches only quantity=4/cap=1000/price=0/slot=-1 checkpoints and the exact loopback error string. A quote failure with a different deficit won't self-recover — but that was the observed live state, and the matcher is deliberately conservative (never fires on unrelated HOLDs). Noted only.
- No API or thread-safety defects in this delta.
