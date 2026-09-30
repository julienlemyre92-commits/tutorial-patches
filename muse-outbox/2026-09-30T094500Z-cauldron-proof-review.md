# Bounded review: drink:cauldron RETRY (Build 553) + HUMAN_PACING acceptance
Date: 2026-09-30 ~05:47 EDT | Muse review-only, no edits/builds/deploys
Sources: source-review/build553-witchspotion/WitchsPotionScript.java,
source-review/questcommon/HUMAN_PACING.md, 05:38:27..05:40:42 frames + diags.
(Note: no 05:38:37-05:38:53 frames/diags exist on GitHub; client.log is PC-side,
not in the repo. Chatbox line at 05:38:50 is the only log evidence I can verify.)

## Q1: Does the cauldron retry indicate a general action-proof issue?

**Yes — a general proof-predicate gap, not a cauldron fluke.** Mechanism from source:

1. `drinkCauldron` (lines 544-551) issues `drink:cauldron` with `Proof.CAULDRON`,
   12000ms timeout: `issue("drink:cauldron", Proof.CAULDRON, f, 12000, 0,
   () -> cauldron.click("Drink-from"));` (line 549).
2. The CAULDRON proof predicate (line 339):
   `case CAULDRON: return f.finished() || f.varp != p.before.varp;`
   It does NOT accept a newly opened dialogue as progress, unlike the
   TALK/DIALOGUE proofs (lines 313-319), which accept
   `f.inDialogue != p.before.inDialogue || f.hasContinue != p.before.hasContinue
   || !f.dialogue.equals(p.before.dialogue)`.
3. The tick gate (line 200): `if (pending != null) { verifyPending(f); return; }`
   starves `dialogue()` (line 222) for the entire proof window. The drink click's
   only observable effect is opening Hetty's dialogue; the quest completes by
   continuing that dialogue — but nothing continues it while pending exists.
4. Live result: click issued ~05:38:38, dialogue opened, sat uncontinued for 12s,
   timeout fired -> `[WitchsPotion] RETRY action=drink:cauldron failure=1/3`
   (05:38:50, visible in chatbox of the 05:40:42 DONE frame). Spurious failure:
   the click had landed. After pending cleared, the next tick hit
   `if (dialogue(f)) return;` BEFORE the varp==2 branch (lines 222/225), continued
   the dialogue, varp 2->3, QuestState FINISHED (05:38:53 per your log).

So the retry was the framework's bounded-timeout machinery behaving as designed
(and correctly NOT treating the timeout as success), but the *predicate* was
under-specified: for drink:cauldron the only proof paths are completion or varp
change, both of which arrive only via the dialogue the predicate ignores.

**General exposure:** any future action whose completion runs through dialogue
frames (turn-in talks, drink/hand actions, NPC handoffs) can spuriously burn a
retry. It was benign here ONLY because the ordering at lines 222-225 lets
`dialogue()` run before the action is re-issued after a timeout. Latent hazard:
a re-issued world click landing while its dialogue is open is the Master Chef
frame-1-reset class of bug (click while dialogue open resets/closes it) — the
ordering saves it today, but it is fragile.

**Minimal recommendation (one line):** line 339 ->
```
case CAULDRON: return f.finished() || f.varp != p.before.varp
        || f.inDialogue != p.before.inDialogue || f.hasContinue != p.before.hasContinue
        || !f.dialogue.equals(p.before.dialogue);
```
i.e. adopt the TALK/DIALOGUE dialogue-open acceptance for CAULDRON. The opened
dialogue proves the click landed; pending clears; dialogue() drives varp 2->3.
No RETRY noise, no re-click-while-dialogue-open risk, no behavior change for
any other action. Optional hardening (not required for this fix): at line 200,
run `dialogue()` before the pending gate when a dialogue is open, so no proof
window can starve dialogue continuation for dialogue-driven completions.

## Q2: HUMAN_PACING.md acceptance vs actual Witch pacing

HUMAN_PACING.md (source-review/questcommon/) says: observe stage/dialogue/
inventory/position/health/scene before acting; one action, prove on a later tick
(click return, proximity, timeout are not proof); after a proved action vary the
next-action delay in a small bounded range (never delay emergency eating or
stall recovery); vary among equivalent reachable targets, keep quest tiles
fixed; no wandering, no repeated unproved clicks, no retries for appearance —
after bounded failures rescan/change approach or HOLD; report target, proof and
pacing reason in diagnostics.

Witch Build 553 complies on every point I can verify:
- `observe()` (lines 231-281) snapshots stage, dialogue, inventory, pos, health,
  scene each tick before acting. Yes.
- One action per tick, later-tick proof via issue()/verifyPending(); click
  return value is logged but never treated as proof. The 05:38:50 timeout is
  exactly the designed bound firing on an under-specified predicate (Q1). Yes.
- Post-proof delay varied: 150-450ms TALK/DIALOGUE, 300-850ms other actions,
  500-1200ms when varp changed (lines 295-300). Emergency eating not delayed:
  `safety(f)` (line 201) runs before the `nextActionAt` gate (line ~203), and the
  SURVIVAL preempt (lines 195-199) clears pending rather than waiting. Yes.
- Targets are unique NPCs/objects (Hetty, cauldron 2024, range 9682); no
  equivalent-target pool, no wandering. Yes.
- Retries bounded (3 -> HOLD, lines 303-308); retry re-issues the same click
  rather than changing approach, but the rule permits "enter a diagnostic HOLD"
  as the terminal option. Borderline-noted, no violation. Yes.
- Diagnostics carry target, proof, pacing reason: ACTION/PROVED/RETRY lines plus
  `paceReason` in status (line ~839). Yes.

Live pacing evidence: 05:34:42 START_HETTY -> 05:38:53 FINISHED (~4m11s, 17 QP,
100% health throughout, error=none on all DONE diags). No per-action timing is
measurable from screenshots, but no >2s unexplained idle appears in the
~30-60s cadence. No pacing defect found.

## Verdict

Ship the one-line line-339 predicate fix before the next quest plugin (it
carries the same framework). No pacing changes needed. Nothing in this run
needs a rebuild of the completed Witch's Potion run.
