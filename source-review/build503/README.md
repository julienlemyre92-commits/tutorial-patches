# Build503/504 Source Review

**Build:** 504 (supersedes 503)
**Patch:** patch-501.zip (supersedes patch-500.zip)
**Published:** 2026-09-29 21:20 EDT

## Files
- `CooksAssistantScript.java` — main quest logic (Build504)
- `CooksAssistantPlugin.java` — plugin lifecycle

## Hashes
See `hashes.txt` for SHA256.

## Patch Mapping
- Build504 → patch-501.zip
- version.txt = 501
- (Build503 → patch-500.zip was superseded before deployment)

## Key Changes
1. **Build503:** Bounded store route to Lumbridge General Store (3209,3247)
2. **Build503:** LOGIN_GATE moved to outer pre-isLoggedIn return
3. **Build503:** Source-driven bucket recovery (ground Bucket or Shop NPC)
4. **Build503:** RESET command for logic-only testing
5. **Build504:** Coins check before purchase — if coins==0, scan for free ground bucket instead of endless Trade/Buy loop

## Lifecycle Notes (per Alex 21:18)
- Plugin disable/enable resets state only if lifecycle cleanup is correct
- Cannot reload JVM-loaded classes; Supervisor restart needed for new bytecode
- RESET command: RESET_REQUESTED → RESET_APPLIED → next-tick proof (logic-only tests)
- Alex investigating MicrobotPluginManager.update/remove/install with URLClassLoader.close for external-plugin replacement (may avoid restarts)

## Route Code Location
- Store route: `doGetBucket()` recovery branch, "Build 503: RECOVERY" markers
- Coins check: inserted at line ~3228, "Build 503: RECOVERY: coins="
- Purchase TODO: Trade → Buy Bucket interface handling not yet implemented
