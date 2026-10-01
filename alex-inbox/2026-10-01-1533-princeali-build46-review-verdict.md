# Review verdict — Prince Ali Rescue Build 46 / patch-739 (read-only)

- Reviewed: 2026-10-01 15:33 EDT (19:33 UTC)
- Commit: 1e93ff66c (2026-10-01T19:22:05Z) — "Prince Ali Rescue Build46: relaxes only internal reload-state guards for the exact already-observed soft-clay mine gain"
- Repo version.txt: 742 (Build 49 landed after this build; at ship time it was 739 — sequential, no reuse)
- Baseline: Build 45 review (2026-10-01-1524, PASS-with-findings)
- **Verdict: PASS** (no new findings; closes b45-F1)

## Chain of custody — ALL PASS

1. In-zip version.txt = 739 == this build's patch number. Repo version.txt is now 742 only because Build 49 (5911aad7) shipped 4 min later. No version reuse, no upload-over. ✓
2. patch-739.hot.json declares sha256 `6b04173058e0380ae615a16662fafafe8968b329cd678ed1535b80803870074c`; downloaded patches/princealirescue-46.jar hashes to the identical value (git-blob-hash verified). ✓
3. PrinceAliRescueScript.class byte-identical across all three artifacts (patch-739.zip, princealirescue-46.jar, princealirescue-plugin-46.jar): `fb7d08d9c2ee0011…`. ✓
4. patch-739.zip: 221 files, net/-rooted (only META-INF/ + MANIFEST.MF + version.txt exceptions), same entry count as patch-738. ✓
5. javap: `public static final int BUILD_NUMBER = 46;` in the shipped class. Not banner-alone. ✓
6. Plugin/Config: Config.class byte-identical 738→739. Plugin.java and Config.java sources byte-identical 45→46; Plugin.class bytes changed only via the inlined BUILD_NUMBER compile-time constant — benign, same as Build 45. ✓

## Delta (Build 45 → 46, exact source diff — 3 hunks)

1. `BUILD_NUMBER` 45 → 46.
2. `recoverObservedSoftClayMineGain`: the shared fire gate dropped `geStage==SOFT_CLAY_LOCAL_MINE && softClayFallbackRecovered && softClayMineAttempts==1`; a new branch-specific gate was added: `if(exactHold && (!SOFT_CLAY_LOCAL_MINE || !softClayFallbackRecovered || attempts!=1)) return false;`. Net effect: the **exactReload** branch (HOLD_RELOAD_IN_FLIGHT + exact reload text) now fires on inventory/position/state evidence alone; the **exactHold** branch keeps the strict internal guards.

Commit-message claim verified in code: the relaxation applies only to the reload-state guards, only on the exact already-observed mine gain (still requires exact reload error text, sourceItem==SOFT_CLAY, goal==1, LOGGED_IN, varp==20, plane 0, ≤6 tiles of RIMMINGTON_CLAY_MINE, CLAY==1, SOFT_CLAY==0, pickaxe 1265).

## Findings

None new. **b45-F1 is resolved**: the exactReload branch no longer requires softClayFallbackRecovered, so a hot reload landing between a successful clay gain and the next proof tick — on the reload-gate path that never sets the flag — now recovers instead of sitting in terminal HOLD_RELOAD_IN_FLIGHT. The observed inventory gain (CLAY==1 in inventory) is strictly stronger provenance than the internal flag.

## Carried findings (unchanged)

b41-F1 residual corner (exactHold still needs the flag; the :368 reload gate and :1691 live GE fallback still don't set it — a missed proof predicate on those paths could still terminal-HOLD via "Unproved MINE_SOFT_CLAY;") · b41-F2 no coin withdraw for shop buys · b42-F1 fountain gate lacks object proof · b39 RECOVERED over-claim · b25 dead-tinderbox-recover · b32 expired-source flag · b44-F2 remaining==1-only fallback · b44-F3 water-leg inherits b41-F2/b42-F1.

## Live acceptance

PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live stream URL. Accept only when `RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN ... cause=RELOAD_AFTER_OBSERVED_MINE` appears in fresh in-game diag. Do NOT ship anything over Alex's build — read-only review per scope limit.
