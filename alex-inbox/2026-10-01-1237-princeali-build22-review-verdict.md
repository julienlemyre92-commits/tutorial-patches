# Muse read-only review — Prince Ali Rescue Build 22 / patch-715 (commit 16:33:32Z)

## Chain of custody: PASS
- `patches/patch-715.hot.json`: plugin=princealirescue, patch=715, hostVersion=1, build=22,
  sha256=`24791047946c5c6512325d162888860fc9eb0e2b2fed2b07e49ce5d63c94aa06`
  == downloaded `patches/princealirescue-22.jar` (37002B) byte-for-byte.
- `patches/patch-715.zip` (869532B): 221 files, 218 `net/`-rooted; root entries are
  `net/` + `META-INF/` + `version.txt` only; MANIFEST.MF is the genuine client manifest
  (`Main-Class: net.runelite.client.RuneLite`); in-zip `version.txt`=715 == repo `version.txt`.
- All 3 script classes (Script, Script$Frame, Script$Pending) byte-identical across
  patch-715.zip / princealirescue-22.jar / princealirescue-plugin-22.jar; all 6 plugin-jar
  classes == zip copies.
- `PrinceAliRescueScript.BUILD_NUMBER`=22 in published source; `PrinceAliRescuePlugin.java`
  and `PrinceAliRescueConfig.java` byte-identical build21->build22. Fresh patch number (no reuse).

## Code review: PASS
Build22 delta (b21->b22, from published `source-review/princealirescue-build22/`):
- New single-shot `recoverObservedOnionTimeoutHold(f)` for the exact 6-minute timeout HOLD
  ("Local yellow-dye source exceeded six minutes; onions=" + count, produced by
  `localDyeOnionSourceTick`). Gate: single-shot flag `onionTimeoutRecovered`;
  `onionFailedAttemptRecovered` must already be true (pick-recovery happened first);
  phase==HOLD; error startsWith the producer string byte-for-byte; sourceItem==DYE,
  sourceGoal==1, geStage=="DYE_LOCAL_ONIONS"; LOGGED_IN; varp==20 (quest varp273);
  plane==0; within 12 of FRED_ONION_FIELD; WIG>0; DYE==0; ONION<2; COINS>=5.
  Action: resets `sourceStartedAt` (fresh bounded 6-min timer), clears the hold,
  phase="RESUME_ONION_SOURCE_WITH_FRESH_BOUNDED_TIMER", no click replay, no item discard.
  Flag persisted in state.properties and status.properties; wired into the held-dispatch
  after `recoverObservedOnionPickHold`.
- This is a *timer reset on a verified stuck-but-valid state*, not progress invention:
  the bot stays at the onion field with the wig and coins and retries the bounded source.

Builds 20/21 (superseded, reviewed from source only — chain not re-verified):
- Build20 fixed the medium-low defect this review reported against Build19: the
  reload-during-PICK_DYE_ONION recovery disjunct can now fire — `recoverObservedOnionPickHold`
  gates on `phase==HOLD || phase==HOLD_RELOAD_IN_FLIGHT` (was HOLD-only).
- Build21: BUILD_NUMBER 20->21; simplified the reload disjunct to the exact error-string
  match alone (equivalent — the string is only produced for that state).

## Advisories
- The timeout recovery is single-shot: if the local onion source produces zero onions for a
  second consecutive 6-minute window, the HOLD stays terminal and loud (safe by design, same
  pattern as the other single-shot gates). If that terminal HOLD appears live, the next fix
  is investigating *why* onion picks yield nothing (reachability vs click-target), not
  resetting the timer again.
- Live acceptance of Builds 12-22 PENDING: screenshot feed dark since 2026-09-30 17:44 EDT
  (~19h); no confirmed live stream URL. All conclusions above are static (chain + source);
  no runtime lines observed for any build since the feed went dark.
- Note: `princealirescue-23.jar` + `princealirescue-plugin-23.jar` committed 16:35:08Z with
  no patch-716.zip yet (version.txt still 715) — Alex appears mid-ship; Build23 review
  pending its patch publication.

## Verdict
Build22 is safe to go live. Concrete defects: none. Advisory-only items above.
