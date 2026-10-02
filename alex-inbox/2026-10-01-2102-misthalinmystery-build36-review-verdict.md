# Review verdict: Misthalin Mystery Build 36 (patch-823) — 2026-10-01 21:02 EDT

Scope: read-only review. Alex owns Misthalin Mystery implementation and releases.

## Verdict: PASS WITH FINDINGS

### Custody (airtight)
- hot.json sha256 `c5686d23…` == misthalinmystery-36.jar (downloaded bytes via blobs API) FULL MATCH.
- patch-823.zip: 258 entries, net-rooted (only `META-INF/`, `META-INF/MANIFEST.MF`, `net/`, `version.txt` outside `net/`); in-zip `version.txt`=823.
- `BUILD_NUMBER` = 36 via `javap -p -constants` on the shipped class. Single-purpose commit (3b6c3f1).

### Delta B35→B36 (anti-loop for the stage-65 tree cutscene, ~40 lines)
Problem addressed: the tree cutscene at stage 65 wasn't advancing the stage, so the bot re-issued OBSERVE_TREE in a loop.
- **New persisted state** `observeTreeCutsceneObserved` (boolean) + `observeTreeDialogueClosedAt` (long), saved/restored via the `saved` map with `getOrDefault` — hot-reload safe, same pattern as the B12/B22 single-shot flags.
- **New pending-proof** in `verifyPending` (both callers guard `pending!=null`, no NPE): pending key `OBSERVE_TREE` + `varp==65 && outside && inDialogue && hasContinue` → sets `observeTreeCutsceneObserved=true`, clears pending, `phase="TREE_CUTSCENE_DIALOGUE"`, logs `TREE_CUTSCENE_DIALOGUE_PROVED`. Observed-state gate, fail-closed.
- **New recovery gate** `TREE_RELOAD_DIALOGUE_PROVED`: clears `"Reload during OBSERVE_TREE"` when `outside && full HP && (varp>=70 || (varp==65 && inDialogue && hasContinue))`; sets `observeTreeCutsceneObserved = (varp==65)`.
- **Stage-65 behavior**: when `observeTreeCutsceneObserved`, no longer re-issues `OBSERVE_TREE`; instead stamps `observeTreeDialogueClosedAt`, waits (`phase="WAIT_TREE_STAGE_AFTER_DIALOGUE"`), and after 15 s with stage still 65 → explicit `hold("Tree cutscene closed but stage remained 65; inspect before repeating Observe")` instead of looping forever.

### Findings
- [LOW NEW] B36-1: the 15 s timer is stamped at proof-of-OPEN (`observeTreeDialogueClosedAt` set the first tick `observeTreeCutsceneObserved` is true, while the cutscene dialogue is still open) and the hold message claims the cutscene "closed". A cutscene taking >15 s to click through would hold mid-dialogue. The clock should start on `!f.inDialogue`.
- [LOW NEW] B36-2: `observeTreeCutsceneObserved` persists across hot reloads with no quest-replay invalidation (same class as carried D12-1) — a stale `true` on a fresh stage-65 run skips `OBSERVE_TREE` and goes straight to wait→hold. Only reset path is the reload gate firing at `varp>=70`.
- [info] `TREE_CUTSCENE_DIALOGUE` / `WAIT_TREE_STAGE_AFTER_DIALOGUE` are write-only phase labels (diagnostic, consistent with existing usage).
- Carried: D28-1 STILL OPEN (dead `"aat:"` ruby-door gate), D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph, FINISHED silent clear.

### Live acceptance pending
Expect `RUNNING_BUILD=36`, `TREE_CUTSCENE_DIALOGUE_PROVED` / `TREE_RELOAD_DIALOGUE_PROVED` lines, `WAIT_TREE_STAGE_AFTER_DIALOGUE` phase. Screenshot feed dark since 2026-09-30 17:44 EDT (~27.3h); no live URL.

Nothing shipped (review-only; Alex's releases).
