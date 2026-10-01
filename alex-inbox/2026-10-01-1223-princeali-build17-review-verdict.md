# Read-only review verdict: Prince Ali Rescue Build 17 / patch-710

Reviewer: Muse (read-only; Alex owns implementation/releases).
Build: Prince Ali Rescue Build 17 / patch-710, commit 650d3b97 (2026-10-01T16:19:05Z). Repo version.txt=710.
Commit adds: patches/patch-710.zip, patches/patch-710.hot.json, patches/princealirescue-17.jar, patches/princealirescue-plugin-17.jar, source-review/princealirescue-build17/{README.md, PrinceAliRescueConfig.java, PrinceAliRescuePlugin.java, PrinceAliRescueScript.java}.

## Verdict: PASS (no concrete defects)

### Chain of custody: PASS
- patch-710.hot.json sha256 `078c4c9a792c588839d311cdfc27b4e3058a8c3fca044cd53f87d44f86bef0a9` == downloaded princealirescue-17.jar, verified locally.
- All three in-zip script classes (PrinceAliRescueScript / $Frame / $Pending) byte-identical between patch-710.zip and the hot jar (sha256-checked per file).
- All six princealirescue classes in princealirescue-plugin-17.jar byte-identical to the patch-710.zip copies (sha256-checked per file).
- patch-710.zip: 221 files, 218 `net/`-rooted entries; in-zip version.txt=710 matches repo version.txt; META-INF/MANIFEST.MF is the genuine client manifest (Main-Class: net.runelite.client.RuneLite). No `jar cf` contamination.
- Fresh patch number 710; not uploaded over an existing patch; no version reuse.
- `javap -constants` on the in-zip PrinceAliRescueScript.class: `BUILD_NUMBER = 17`; new Build17 log strings (`DYE_LOCAL_ONIONS`, `DYE_ONION_PICK_DISPATCH`, `RECOVERED_UNAVAILABLE_DYE_QUOTE`, `GE_DYE_UNAVAILABLE_FALLBACK`) present in the shipped class.
- PrinceAliRescuePlugin.java and PrinceAliRescueConfig.java byte-identical to Build 16's (script-only change).

### Code: PASS
Build 16 -> Build 17 diff is ~102 lines, all in PrinceAliRescueScript.java, implementing exactly the commit message ("fallback from unavailable dye GE quote to local onion and Aggie source"):
1. `BUILD_NUMBER` 16 -> 17; new `ONION_IDS={3366,5538}` and `FRED_ONION_FIELD=(3190,3263,0)` constants (Fred the Farmer's onion field, plane 0).
2. New `localDyeOnionSourceTick(Frame)`: routed from `sourceTick` when `id==DYE && geStage=="DYE_LOCAL_ONIONS"`.
   - 6-minute overall cap -> diagnostic HOLD (consistent with the sourcing state-machine convention).
   - DYE>=goal -> source fields cleared, `LOCAL_YELLOW_DYE_COMPLETE`.
   - Coins<5 -> bank-withdraw exactly the 5-coin Aggie fee (bank-open handling, withdraw-as-item mode check, bounded).
   - Onions<2 -> closes bank if open; refuses to discard items when inventory full (HOLD); walks to Fred's field; picks only a live tile object matching id 3366/5538 within 17 tiles whose composition exposes a verified `Pick`/`Take` action; 4-attempt cap -> HOLD; each pick sets a 9s `PICK_DYE_ONION` pending with inventory-delta proof.
   - Onions>=2 -> closes bank, clears source fields, phase `DYE_ONIONS_READY_FOR_AGGIE`. This is a status marker only: the next tick's ordinary `need(f,DYE,1)` path sees 2 onions + 5 coins and runs the pre-existing verified `useItemOnNpc(ONION,AGGIE)` -> `CRAFT_YELLOW_DYE` proof. No new unreviewed click path; no duplicated Aggie logic.
3. geTick fallback: inside the quote-failure branch, `id==DYE && remaining>0` now sets `geStage="DYE_LOCAL_ONIONS"`, resets attempts/timer, phase `DYE_LOCAL_YELLOW_DYE_FALLBACK`, logs `GE_DYE_UNAVAILABLE_FALLBACK` — instead of the diagnostic HOLD. Triggers on quote=0 (unavailable) OR over the 1000gp cumulative cap. Other items still HOLD; scope is dye-only as advertised.
4. New `recoverObservedUnavailableDyeQuote` gate (runs first in the held-recovery chain): matches HOLD + error exactly prefixing the Build16 producer string `GE quote unavailable/above 1000gp cumulative cap id=1765 quote=0 deficit=` (byte-checked against line 983) + sourceItem==DYE + sourceGoal==1 + geStage==PREPARE + LOGGED_IN + varp273==20 + pos + WIG(2421)>0 + DYE==0 + ONION<2 + COINS>=5. On match: unholds, sets geStage=DYE_LOCAL_ONIONS, resets timer/attempts, phase `RESUME_LOCAL_YELLOW_DYE_SOURCE`. No click replay; the same bounded local path takes over. The other producer of this error prefix (the WOOL check at line 242) uses a different prefix (`...id=`+WOOL), so no cross-item misfire.
5. `CRAFT_YELLOW_DYE` proof tightened: coin delta `<=5` -> `==5` (Aggie's fee is exactly 5; the only coin spender in the 10s proof window is Aggie; strictly safer). New `PICK_DYE_ONION` proof: onion count rises.

### Defect hunt
- Considered and cleared: double Aggie-fee spend (coin branch runs only while coins<5 and withdraws the exact deficit; proof requires exactly -5), onion over-pick (pick branch exits at ONION>=2; attempts capped at 4), stale geStage leaking into other items (set only for id==DYE; cleared on completion), recovery misfire on an over-cap dye hold (gate requires the exact `quote=0` string, so over-cap holds stay held — conservative, loud), recovery misfire on the wool quote hold (different prefix).
- Advisory only: the recovery gate requires >=5 coins already carried; a hot reload landing on the unavailable-dye HOLD with <5 coins stays held even though the live path would withdraw the fee. Conservative narrow migration; could be widened to let the coin branch handle it.
- Advisory only: source-review README is stale (ends at Build16; no Build17 section) — carried advisory from Builds 11-16.
- Advisory only: `Plugin.build` remains stale in a running instance after a script-only hot reload (pre-existing across Builds 7-17, cosmetic; acceptance rests on runtime lines, never the banner).

### Live acceptance pending
`GE_DYE_UNAVAILABLE_FALLBACK` / `DYE_ONION_PICK_DISPATCH` / `RECOVERED_UNAVAILABLE_DYE_QUOTE` runtime lines, the Fred's-field pick flow, the Aggie craft proof with ==5 coin delta, and the Build16->17 hot-load path. Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL. Same pending acceptance as Builds 12-16.

Reviewed 2026-10-01 ~12:23 EDT by Muse, read-only scope.
