# Verdict: Ernest the Chicken Build 20 (2026-10-01 00:06-00:14 EDT)

Read-only inspection of patch-622 (`patches/patch-622.zip`, `patches/ernestthechicken-20.jar`, `patches/patch-622.hot.json`). No edits, no deployment, no game input.

- IDENT: hot.json = {plugin: ernestthechicken, patch: 622, build: 20, hostVersion: 1, sha256: 494c54ebbbee82893aba416cd8b1bc0bed9544a9777b1fee1344eac301bdfa91} — EXACT match of ernestthechicken-20.jar (32129B, +149B vs build 19).
- ZIP: patch-622.zip, 208 entries, all class paths under net/ root, META-INF/MANIFEST.MF intact (Main-Class: net.runelite.client.RuneLite), version.txt=622 inside (repo version.txt now reads 622 — ship complete).
- IDENTITY: all 6 Script classes (ErnestTheChickenScript + Frame/Pending/LoginFrame/SkillLevelReview/DoorCandidate) byte-identical zip<->jar. Plugin/Config zip-only = expected hot-reload split.
- MARKER: runtimeBuild() = bipush 20, ireturn (same slot).
- DRIFT (621->622, javap -p -c): confined to the main Script class. Inner classes Frame/Pending/LoginFrame/SkillLevelReview/DoorCandidate signatures identical; string constants identical; external API refs IDENTICAL (zero new microbot-base.jar / net.runelite surface). Precise change: new proof predicate `lambda$proved$19(DoorCandidate)` = `id==11471 || (close && !open)` — the SAME shifted-open-state test as Build 19's proof but WITHOUT the `candidate.pos.equals(pending.extra)` position-equality requirement. Build 19's `lambda$proved$18(Pending, DoorCandidate)` only accepted a verified-open door when its candidate sat exactly on the pre-recorded tile; the rendered open door (id 11471) shifts position, so Build 20 accepts the shifted-open state by id/flags alone. Matches commit 04:06:53Z "Build20: verify shifted open panelled door state". Candidate scan is already locally scoped, so the position-free acceptance is bounded — no false-positive concern worth flagging.
- HYGIENE: version sequence 621->622 clean; no duplicate patch-622 paths; no pre-existing ernest21 jar.
- DEFECTS: none. PASS.

- LIVE ACCEPTANCE PENDING: screenshot feed dark since 2026-09-30 17:44:02 EDT (~388 min, zero ERNEST_* frames ever); Build 20 acceptance triggers = fresh RUNNING_BUILD=20 banner, OPEN_PANELLED_DOOR diag lines, or first Ernest screenshot.
