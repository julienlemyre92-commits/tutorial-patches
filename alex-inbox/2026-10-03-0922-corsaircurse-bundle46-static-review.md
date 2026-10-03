# Corsair Curse provider-bundle-46 (patch-1105) static review — Muse, 2026-10-03 09:22 EDT (read-only)

Ship: patch-1105 / provider-bundle-46.jar, commit 2dcac5e946, 09:20:45 EDT.
Script unchanged (still Corsair Curse Build54).

## Sha-chain verification (all pass)
- jar sha256 c88e2f35c46453608b0ec0f865e907ac1ced0f65eac9ee5eb9da0a9eb983627f
  == hot.json artifactSha256 == properties artifactSha256.
- Spot-checked classes vs manifest: NavigationService.class 4f794a2a…,
  NavigationMicrobotDriver.class 1693ff95…, EmergencyLocalEscape.class 7733598b… — match.
- generation 45→46; parentAbiSha256 unchanged (d8911107…); DefaultProviderBundleFactory
  source byte-identical to gen-45 (class-sha delta is a recompile artifact);
  ProviderImplBuild change is the build marker only.

## Delta vs gen-45 (decompiled NavigationService.class diff, normalized)
Exactly ONE semantic change, in tick():
- Navigation budget exhaustion: `requestStop("navigation deadline exceeded", HOLD)`
  → `requestStop("NAVIGATION_BUDGET_EXHAUSTED: destination unproved; workers stopped", UNAVAILABLE)`.
- The terminal path still proves both workers stopped before releasing the lease.

## Assessment: PASS
This directly addresses the "sticky nav-driver wedge" class flagged 08:37 on bundle-43:
a budget-exhausted navigation can no longer wedge the whole run in terminal HOLD —
the lease returns as UNAVAILABLE so the caller can recheck health and bounded-replan.
Consistent with the service's own architecture comments. No new workers, no new
input paths, no lease-ownership changes.

Carry-forward (unchanged, not new defects): EMERGENCY_HOLD mid-emergency still routes
to HOLD; stop() still early-returns while emergency is active (beginEmergency's
ordinary-quiescence gate bounds the window). Open note from 09:11 stands:
GearUpgradePlanner gear-goal bonus key "STRENGTH" matches no item-bonus field —
worth confirming the intended key.

## Live acceptance still pending
No live evidence: screenshot feed dark since 2026-09-30 17:44 EDT (~63.6h);
Bumba stream URL removed by uploader (flagged, once-per-cause). Last live eyes
08:24–08:26 EDT: RUNTIME BUILD 50 parked Lumbridge Castle steps, 12/50 checkpoints,
HP 25/25, FOOD 0. Watch for: gen-46 hot-load → banking pass → gear pass →
checkpoints 12/50→14+ → proved meals → Ithoi re-engagement. Accept only from
fresh runtime lines.
