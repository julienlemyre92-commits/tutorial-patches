# Review verdict — Prince Ali Rescue Build 70 (patch-763) — READ-ONLY, Alex owns releases

**Date:** 2026-10-01 ~18:12 EDT (muse review loop)
**Commit:** `80a78967` "Prince Ali Rescue Build70: confirm live key furnace question once with material proof"
**Verdict: PASS WITH FINDINGS** (read-only; nothing shipped from this side — Alex owns Prince Ali Rescue implementation/releases)

## Custody chain — CLEAN
- `patches/patch-763.hot.json`: `{plugin: princealirescue, patch: 763, build: 70, sha256: 7f636fe5d945547ec07400115d509d2eb47586f7079cb3be608cdf7744bdcf9c}`
- `patches/princealirescue-70.jar` (13 script classes) sha256 = `7f636fe5...f9c` == hot.json ✓ (fetched bytes, exact)
- `patches/patch-763.zip`: 922368 bytes, 216 classes, `net/`-rooted monolithic overlay; in-zip `version.txt` = `763` ✓
- All 13 script classes byte-identical zip ↔ script-jar; 3 config/plugin classes byte-identical zip ↔ `princealirescue-plugin-70.jar` (new artifact: Config+Plugin split out of the hot-reload script jar — matches the host rule that plugin/config changes need a jar reload, not a hot swap); script classes byte-identical plugin-jar ↔ script-jar ✓
- `javap -constants`: `BUILD_NUMBER = 70` ✓
- Single-purpose commit (hot.json + patch-763.zip + 2 jars + version.txt + source-review/ + nothing else) ✓
- `version.txt` on repo = `763` ✓

## Delta 69 → 70 (source-review diff, +22 lines, script only)
- New persisted flag `keyFurnaceConfirmed` (state map: save + restore + put) — hot-reload-safe (wydinEmployed lesson applied).
- New `Rs2Dialogue.getQuestion()` capture into `Frame.question`.
- New `handleKeyFurnaceConfirmation(f)` at tick top (after membership promo): engages only if `!keyFurnaceConfirmed` AND (`MAKE_BRONZE_KEY` pending OR timed-out held with `error.startsWith("Unproved MAKE_BRONZE_KEY;")`) AND logged-in AND varp273==20 AND within 6 tiles of (3273,3184,0) AND `count(KEY_PRINT)==1` AND `count(BRONZE_BAR)>=1` AND `count(BRONZE_KEY)==0` AND `options.equals("Yes|No|")`.
  - Logs `KEY_FURNACE_CONFIRMATION question=<q> options=<o> player=<pos>`.
  - If question (lowercased) lacks "key" → HOLD `Unexpected furnace confirmation question: <q>`.
  - Else: `keyFurnaceConfirmed=true`, clears held/error, `clickOption("Yes")` → re-sets `MAKE_BRONZE_KEY` 15s pending with consumed-materials proof (BRONZE_KEY up + KEY_PRINT down + BRONZE_BAR down); on click reject → HOLD `Verified key furnace Yes dispatch rejected`.

## API / thread-safety (verified against installed `~/workspace/microbot-base.jar`)
- `Rs2Dialogue.getQuestion()`, `getDialogueText()`, `clickOption(String)` all exist in `util/dialogues/Rs2Dialogue` ✓
- Frame read inside `Microbot.getClientThread().invoke(Supplier)` — matches the established B65/66 client-thread-dispatch pattern; no Build-517-class off-thread risk ✓
- The `timedOut` prefix matches the generic pending-timeout writer exactly: line 675 `hold(f,"Unproved "+pending.action+"; ...)` → recovery gate is not dead code ✓
- Proof predicate (line ~3512): key up + print down + bar down; the re-set `before` snapshot is taken at the "Yes" click, which is the correct material baseline (useItemOnObject alone doesn't consume; the craft consumes at "Yes") ✓
- Placement: handler runs each tick but returns false unless all gates hold; after confirm+Yes, `keyFurnaceConfirmed=true` lets the tick fall through normally ✓

## FINDINGS
1. **[NEW — MEDIUM conditional] Furnace-question readability risk.** If `getQuestion()` returns empty/unreadable for the furnace Yes|No prompt (documented pattern: `getQuestion()`/`getDialogueText()` are often EMPTY for tutorial-style dialogue widgets), `contains("key")` is false → HOLD `Unexpected furnace confirmation question: ` on the FIRST sighting of the prompt, and the `awaiting` gate (pending still `MAKE_BRONZE_KEY`) re-enters the handler every tick → permanent HOLD, bot never clicks Yes. Suggest: HOLD only when the question is non-empty AND lacks "key"; treat empty-question as soft-proceed (log + click Yes), or pin the observed live question string in the source comment. Live acceptance PENDING (feed dark since 2026-09-30 17:44 EDT; no live URL) — awaiting the new `KEY_FURNACE_CONFIRMATION question=...` diag line.
2. **[NEW — LOW] Flag set before dispatch.** `keyFurnaceConfirmed=true` precedes `clickOption("Yes")`; on rejection the terminal HOLD (`Verified key furnace Yes dispatch rejected`) persists with the flag set (reload-persisted), so no re-confirmation path exists on a manual resume. Acceptable per single-shot design — noting only.
3. **[LOW carried] source-review README is the stale Build-1 template** (RUNNING_BUILD=1..18 notes, build-1 shas) — doc-only; Build 70's README carries the same template. Suggest regenerating from the template-fix once.
4. **[MEDIUM conditional carried] banked-pickaxe 1265 gap** (uncertain whether the banked pickaxe path covers item 1265; carried from B69 review).
5. **[LOW carried]** B68 second-respawn held-requirement near-unreachable; B67 partial-set direct-loot gap; dead-code Shantay resume; members-world gate by design.
6. **[LOW carried from B69 review]** Osman exit exact-string vs error-branch `contains()` inconsistency on the same options string.

## Live acceptance — PENDING
Awaiting: `KEY_FURNACE_CONFIRMATION question=... options=Yes|No|` diag line, then the `MAKE_BRONZE_KEY` proof (BRONZE_KEY gain). Screenshot feed dark since 2026-09-30 17:44 EDT; no live URL confirmed — Alex's runtime reports are the only live source.

**Method:** custody via git-blobs raw API + local unzip/sha256 + javap against installed JDK 17; semantic diff of source-review script sources; API signatures verified against installed microbot-base.jar bytecode. No edits, no ships, no code touched — read-only review.
