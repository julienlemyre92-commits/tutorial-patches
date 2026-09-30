# World-list endpoint probe — 2026-09-30 12:11 EDT (Muse, review-only)

Direct answer to the open 11:37 EDT ping question 3 ("Has the world-list situation changed at all since the 08:38 world-lookup failures?").

**Probed from the open internet (this worker's network) at 12:11 EDT:**

| URL | Result | Time |
|---|---|---|
| `https://oldschool.runescape.com/jav_config.ws` | HTTP 302 → 200, valid config payload (2,766 bytes, `initial_jar=gamepack_7660616.jar`, `codebase=...oldschool146.runescape.com`) | ~2.1s total |
| `https://www.runescape.com/slr.ws` (world list) | HTTP 200, 30,000-byte world list, live host entries (`oldschool168.runescape.com`, etc.) | ~0.9s |

**Verdict: there is NO Jagex-side outage.** Both endpoints are healthy and fast from the general internet. The jav_config.ws timeout observed on Julien's PC (bundled JRE + PowerShell, since ~08:38 EDT) is **PC-local**. Candidate causes, in rough likelihood order:

1. DNS resolution on the PC (try `nslookup oldschool.runescape.com` / `Resolve-DnsName`).
2. The bundled JRE's TLS stack failing the handshake (probe the same URL from PowerShell `Invoke-WebRequest` to separate JRE vs OS-network).
3. Firewall/ISP-level blocking or transparent proxy on his route.
4. The launcher's own config module (Microbot ClientLoader world-lookup path) misreading a recovered endpoint — Julien's pro tip stands: the game update may have broken the injected client's config fetch even though the endpoint is fine.

Suggested isolation on the PC (Julien or Alex, no bot code changes):
- PowerShell: `Invoke-WebRequest https://oldschool.runescape.com/jav_config.ws -TimeoutSec 15` — if this works, blame the JRE/client module, not the network.
- If PowerShell also times out: `nslookup`, then try a phone-tether hotspot to rule out the ISP.
- If the endpoint responds on the PC but the client still can't log in, the suspect moves to the injected client's world-selection code path.

**Game-state note:** this doesn't change the WAIT_LOGIN picture by itself — it just rules out "Jagex is down" as the explanation. The unmitigated gap (Pirate Build 557 has no jav_config-independent world fallback, unlike the Cook's Assistant slr.ws path) remains the reason the client can't get past the login gate.

Muse review-only — no edits, no builds, no deploys.
