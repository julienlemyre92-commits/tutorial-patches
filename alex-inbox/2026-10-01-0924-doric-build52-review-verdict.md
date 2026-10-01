# Doric Build 52 (patch-694; supersedes patch-693) -- review verdict: PASS (1 informational)

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-694.zip (Build 52, commit 4f0c76c3 13:21:50Z "Doric Build52: correct hot reload build marker"), superseding patch-693.zip (commit 8d6c98fe 13:19:22Z), vs patch-692.zip (Build 51, reviewed PASS 13:22Z). version.txt=694 at review time.
- **Method:** git-blobs API (Accept: application/vnd.github.v3.raw) download of patch-694.zip (824,113 bytes) + patch-693.zip (824,112 bytes) + both hot.json + patches/doricsquest-52.jar (31,671 bytes, the 694 build) + patches/doricsquest-plugin-52.jar (39,958 bytes). unzip entry lists IDENTICAL 693<->694 (215 entries, 212 net/-rooted; version.txt=694 in and out of the zip). javap -p -c three-way diff (692/693/694) of DoricsQuestScript; Plugin diff. Chain-of-custody: patch-694.hot.json sha256 1f850567edc18915... == patches/doricsquest-52.jar == the 4 script classes inside patch-694.zip (jar==zip byte-identical). NO patch reuse (693->694 is a fresh number), NO overwrite of any patch-N.zip.

## What happened: a lying marker, self-corrected in 148 seconds

- **patch-693 shipped Build 52's logic with `runtimeBuild()=51`** (plus BUILD_NUMBER init, the RUNNING_BUILD banner, and the Plugin marker all still 51) -- the exact stale-marker failure class this loop watches for (cf. Doric Build 10's RUNNING_BUILD=8, Build 40's marker 39). Had it gone live, every diag line would have blamed Build 51 for Build 52's behavior.
- **patch-694 (148s later) is a pure marker correction:** the 693->694 Script diff is exactly four `bipush 51`->`bipush 52` changes and nothing else; the Plugin diff is one `bipush 51`->`bipush 52`. `runtimeBuild()=52` now, honest banner. Correct handling: fresh patch number, no overwrite. The superseded patch-693 was live for <3 min and is unreviewable after the fact only in the sense that patches/doricsquest-52.jar was overwritten by the 694 build -- but 694's bytes are fully verified, which is what runs.

## Delta: Build 51 -> Build 52 -- HOLD recovery for the iron-capacity dead end

Single functional change (692->693, carried intact into 694), a new intercept at the very top of `tick()`, ahead of the existing routing:

- **Guard:** phase=="HOLD" AND error startsWith "Inventory full; preserve existing valuables, no auto-deposit" AND Frame.quest==IN_PROGRESS AND Frame.varp==10 AND Frame.mining>=15 AND counts[2]<NEEDED[2] (iron still short) AND Rs2Inventory.isFull() AND itemQuantity(1931)>0.
- **Action:** clears the hold (`held=false`, `error=""`), sets `phase="DORIC_IRON_CAPACITY_RECOVERY"`, logs `[DoricsQuest] RECOVER_IRON_CAPACITY_HOLD quest={} iron={}/{} item={} decision=DROP_ONE_LOW_VALUE_POT`, writeStatus, return.

This is the companion to Build 51's drop intercept: Build 51 drops a pot when the inventory fills *during* getMaterials; Build 52 *recovers an already-held* inventory-full HOLD. End-to-end path verified by reading the dispatch: on the next tick phase!=HOLD so the recovery doesn't refire; `DORIC_IRON_CAPACITY_RECOVERY` matches no routing check, so tick() falls through to `firstMissing()`->`getMaterials(frame, 2)` where Build 51's intercept fires (iron missing, mining>=15, still short, full, pot present) and Drops one pot via `DROP_LOW_VALUE_CAPACITY_POT` (9000ms proof). After the drop isFull clears and iron mining proceeds. No livelock: recovery is one-shot per HOLD (phase leaves HOLD), the drop is one-shot per full episode.

- **Frame/LoginFrame/Pending 692->694:** not re-diffed (693->694 Plugin/Script diffs already isolate the only changes; 692->693 Script hunks beyond the recovery block are branch-target shifts from the insertion plus constant-pool comment-width noise -- verified a sample, no functional content).
- **All API calls** in the new block (Rs2Inventory.isFull/itemQuantity, QuestState comparison, Frame fields) are signatures the class already uses -- no new NoSuchMethodError surface.

## Findings

- **[i] `DORIC_IRON_CAPACITY_RECOVERY` is write-only.** The phase string is set in tick() but never read by any dispatch check -- it serves as a status-file label only, and the recovery works because tick()'s fallthrough reaches firstMissing()/getMaterials() regardless of phase. Fine as designed, but if a future change makes the tick dispatch phase-exhaustive (unknown phase -> HOLD), this label would need a routing arm.

## Verdict: PASS

Correct packaging, verified hot chain, honest markers after the 148-second self-correction, and a well-guarded recovery: the HOLD-clear requires all seven conditions (right error text, quest in progress, varp 10, mining 15+, iron still short, full inventory, pot present), preserves everything except the intended low-value pot, and can't loop. Live verification pending (screenshot feed dark since 2026-09-30 17:44:02 EDT -- no DORIC_* frames ever): acceptance lines are `[DoricsQuest] RECOVER_IRON_CAPACITY_HOLD ... decision=DROP_ONE_LOW_VALUE_POT` followed by `DROP_LOW_VALUE_CAPACITY_ITEM ... decision=FREE_ONE_SLOT_FOR_IRON`, and the `RUNNING_BUILD=52` banner. Per the standing rule, Alex's direct in-chat runtime reports supersede cron conclusions.

Nothing shipped (review-only; Alex's releases).
