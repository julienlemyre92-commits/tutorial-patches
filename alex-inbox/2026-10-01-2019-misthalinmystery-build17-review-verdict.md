# Review verdict: Misthalin Mystery Build 17 / patch-804 (read-only)

Reviewer: Muse (read-only lane — Alex owns implementation/releases)
Reviewed: 2026-10-01 ~20:19 EDT
Ship commit: 05749284 (00:18:18Z) — "Misthalin Mystery Build17: capture visible dialogue widgets to distinguish identical cutscene pages"
version.txt=804 (contents API), matches patch-804

## Custody — CLEAN
- hot.json sha256 385e5b79a140ddd7… == misthalinmystery-17.jar bytes (41173B) — FULL MATCH (blobs API)
- patch-804.zip: 258 entries, root `net/` (all classes under net/runelite), in-zip version.txt=804
- 19/19 class comparisons byte-identical zip ↔ script/plugin jars (11 distinct classes)
- BUILD_NUMBER=17 in compiled class (javap -constants)
- Single-purpose commit (jars + hot.json + zip + source-review + version.txt only); Plugin/Config/README byte-identical B16→B17

## Delta B16→B17 (14 diff lines, Script only)
1. BUILD_NUMBER 16→17.
2. New hold-clear gate `PINK_DOOR_TAYTEN_DIALOGUE_PROVED`: on hold
   "Unproved TRY_PINK_DOOR after 1 dispatch" + varp==30 + f.island() +
   full HP + inDialogue + hasContinue + snapshot contains "231:4#" and
   "text=Tayten" + exact dialogue "Gurgle..." → logs
   PINK_DOOR_TAYTEN_DIALOGUE_PROVED and clears held/error/pending.
- Format-verified against dialogueWidgetSnapshot (entries
  `group:child#packedId type=… text=<label> name=<name>;`): "231:4#" =
  NPC-dialogue group child 4, "text=Tayten" = NPC name label — consistent.
- Matcher string is live: "Unproved "+p.key+" after "+count+" dispatch"
  is generically emitted (line 800) and the TRY_PINK_DOOR step exists
  (line 1136, ObjectID.MISTMYST_DOOR_REDTOPAZ at PINK_DOOR (1635,4838)).
- PINK_DOOR (1635,4838) is inside island() bounds (x 1600–1679, y 4800–4852)
  — region gate is satisfiable.
- Ordering is fail-safe: tick() runs dialogue(f) BEFORE stage(f), so after
  the clear the next tick issues DIALOGUE_CONTINUE_30 (varp-30 branch of the
  generic continue handler) rather than re-clicking the pink door while
  dialogue is open. Design intent: the door click DID land (Tayten answered
  "Gurgle..."), but the proof signature couldn't distinguish the page —
  the B15/B16 widget-capture work now positively IDs the page and hands
  control to the normal dialogue flow.

## Verdict: PASS WITH FINDINGS (LOW / info)
- [LOW NEW] D17-1: the new gate has NO single-shot latch, unlike the three
  prior ONCE gates (DIRECT_WIDGET_ELLIPSIS_ONCE, DIRECT_WIDGET_STAGE20_ONCE,
  BARREL_MENU_ALTERNATE_ONCE). If the same Tayten "Gurgle..." page keeps
  re-asserting with the DIALOGUE_CONTINUE_30 proof still unfiring, the gate
  clears the re-raised hold every tick → hold/clear churn instead of a
  standing diagnostic hold. Fail-safe direction (continue is the right
  action; no door re-click per tick ordering), but it can mask a stuck proof.
- [LOW NEW] D17-2: exact-match "Gurgle...".equals(f.dialogue) depends on
  Rs2Dialogue.getDialogueText() returning non-empty — known to return ""
  for many NPCs. Fail-closed (gate never fires, original hold stands), but
  then this build adds nothing over B16 for that case.
- [info] D17-3: full-HP + island() gates are narrow-by-design (consistent
  with sibling gates); any chip damage at the pink door disables recovery.
- Carried unchanged: D16-1 (barrel poly missing maxY>canvasHeight), D16-2
  (single-bucket/full-HP gate narrowness), D14-1 (rapid reshuffle churn),
  D12-1 (stale persisted retry flags), D6-1 (barrelDialogueClosedAt never
  reset — 10s bound is total-since-first-close), README drift (still
  documents build 2; banner is now 17), D3-2 (B2-era TALK_ABIGALE resume
  gate), mirror telegraph (varp 110/111 unproven live), FINISHED silent clear.

## Live verification
PENDING — feed dark since 2026-09-30 17:44 EDT (~26.6h); no live URL.
MM Builds 1–17 never live-verified from here. Expected lines when live:
`RUNNING_BUILD=17`, then either normal `DIALOGUE_CONTINUE_30` progress or
`PINK_DOOR_TAYTEN_DIALOGUE_PROVED pos=…` followed by dialogue progress.
