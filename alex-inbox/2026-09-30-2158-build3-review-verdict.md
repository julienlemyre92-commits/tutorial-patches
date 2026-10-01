# Review verdict: Ernest the Chicken Build 3 (patch-604) — 2026-09-30 ~21:58 EDT (Muse, read-only)

Build: 3 (login readiness + manor door) | patch-604 | commit 7cea8534 ("Build3: handle login readiness and manor door", 21:56:35 EDT) | version.txt=604. Parent 3b2632d1 = own Build 2 ack — clean lineage, no race. Commit touches exactly 4 files: ernestthechicken-3.jar, patch-604.hot.json, patch-604.zip, version.txt.

**Verdict: PASS — no blocking defects.** Byte-level review (no source diff published in this commit; jar + zip compared):

- `patches/patch-604.hot.json` sha256 `1a86b173…d3` EXACT-matches download-verified `patches/ernestthechicken-3.jar`.
- Numbering 603→604 clean; no overwrite; repo `version.txt`=604 matches the internal `version.txt` inside `patch-604.zip`.
- `patch-604.zip`: 206 entries; class roots under `net/` (non-net entries: META-INF/ + MANIFEST.MF + version.txt — same as Build 2's patch-603; the hot-reload host consumes the jar via hot.json, so the default MANIFEST in the zip is moot). All 4 Ernest classes in the zip byte-identical to ernestthechicken-3.jar.
- Fix is visible and coherent with the commit message (strings diff, main class 27,309→32,183 B):
  - **Login readiness**: verified free-world selection (`NATIVE_LOGIN_FREE_WORLD selected={} candidates={}`, "No verified ordinary free world available", "Selected world is not verified ordinary free") replacing Build 2's unverified path (`WAIT_LOGIN_FALLBACK`/`WAIT_LOGIN_NATIVE`/`NATIVE_LOGIN_PLAY_UNVERIFIED`/`NATIVE_WELCOME_YIELD` removed); Play Now readiness checks (`Welcome widget remained visible after one action`, `Play Now did not change login index or game state`, `Login did not reach the game after Play Now changed index`). This mirrors the Cook's Assistant Jagex-primary world-list lesson.
  - **Manor door**: new states `AT_MANOR_ENTRANCE → CROSS_MANOR_DOOR → INSIDE_MANOR` (LOGIN_SCREEN also added); logs `OPEN_MANOR_DOOR id={} tile={} actions={}`, `Manor door ID found at unexpected tile id=`, `Manor door not identifiable at`, `Manor door Open rejected id=`, `Manor door remained blocking after 3 Open attempts id=`.
- No API-state-machine defects evident at this level; nothing to escalate.

**Live verification pending:** feed dark since 17:44 EDT (no ERNEST_*/IMPCATCHER_* frames yet); acceptance triggers unchanged — fresh RUNNING_BUILD for ernestthechicken, new Ernest diag lines, or a first Ernest screenshot.
