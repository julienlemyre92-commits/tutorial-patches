# Prince Ali Rescue Build 63 review verdict (Muse, read-only)

- **Verdict: PASS WITH FINDINGS (lows only; carried-forward medium now MOOT)**
- **Build:** Prince Ali Rescue Build 63 / patch-756 (commit 45393d79, 2026-10-01T21:36:15Z). version.txt=756.
- **Custody:** clean. hot.json sha256 `f941f6bdb021b987c9444d8319d8d544e9f31e45f02c7a25d259f0d7edb3b9fe` matches princealirescue-63.jar (67028 bytes) byte-for-byte (git blobs API, Accept: application/vnd.github.v3.raw). BUILD_NUMBER=63 in source-review (line 54). Single-purpose commit (hot.json, patch-756.zip, princealirescue-63.jar, princealirescue-plugin-63.jar, source-review/princealirescue-build63/, version.txt).

## Delta 62 -> 63 (free-account source policy + popup-proof persistence)

Diff is 57 lines, all in PrinceAliRescueScript.java (Config/Plugin byte-identical to Build 62's):

1. **FREE_TO_PLAY_ACCOUNT=true** constant (line 56).
2. **Popup click/proof state survives hot reload** (the known hot-reload memory-reset lesson, applied): new fields `memberPromoPriorHeld` / `memberPromoPriorPhase` / `memberPromoPriorError` / `memberPromoClickedAt`, saved to `state` per tick (lines 229-232) and restored on hydration (lines 322-325). After-reload `DISMISS_MEMBER_PROMO` branch: promo gone -> restore prior held/phase/error, log `PROVED DISMISS_MEMBER_PROMO afterReload=popupHidden loggedIn=true`, then resume; promo still visible >6s after the pre-reload click -> terminal HOLD "Membership promo persisted across reload after one dismissal; no replay". Bounded, no replay. The 6s budget is wall-clock from the pre-reload click, so it correctly spans the reload.
3. **beginSource F2P item gate** (~line 1181): `ItemComposition.isMembers()` via `Microbot.getClientThread().invoke(Supplier)` (the blocking variant -- correct per API; the tick runs off the client thread). `membersItem==null||membersItem` -> HOLD "F2P acquisition rejected: item=... membership=...; choose a verified free item/source". Conservative fail direction (unknown item def = rejected, never silently acquired).
4. **bronzeBarShantayTick F2P gate** (lines 1345-1348): FIRST statement -> HOLD "F2P source unavailable: Shantay Trade recreated the membership prompt; use a free bronze-bar source". Verified single choke point: line 1240 (`id==BRONZE_BAR && "BAR_SHANTAY_SHOP".equals(geStage)`) is the only dispatch into `bronzeBarShantayTick`, and every Shantay path (reachable-interaction tick, open/buy sequence) sits inside it, AFTER the gate. The policy cannot be bypassed.

## Findings

- **[LOW, new, cosmetic]** `resumeShantayAfterMembershipPromo` (line 732) still computes a reachable interaction tile and logs "exactly one post-dismissal attempt reserved" even though the F2P gate fires first on the only path in -- the resume is dead code under FREE_TO_PLAY_ACCOUNT=true. No behavioral harm (no banner recreation is possible), but the log line will mislead anyone reading diag into expecting a Shantay attempt. Suggest gating the resume on `!FREE_TO_PLAY_ACCOUNT`.
- **[CLOSED] carried-forward medium (shop-open-during-approach `!f.shop` exemption): MOOT in Build 63.** The line-1394 gate still lacks the exemption, but it is unreachable: the F2P gate at line 1345 fires first on every entry. Closing as superseded-by-policy; it resurfaces if the flag ever flips or a members path is added.
- **Recorded (Alex-reported, not independently verified -- feed dark):** your Build 63 README notes "Build62 proved two automatic membership-banner dismissals in PID6300. Trading with Shantay recreated the banner; the shop stayed closed." Taken as your runtime report; this is what motivates the F2P rejection. Note the PID differs from the 18:34 EDT known state (19476) -- implies a client restart; unverifiable from this side.

## Live acceptance

PENDING. Feed dark since 2026-09-30 17:44 EDT (~24h); no live URL. Prince Ali Builds 3-63 never live-verified from this side. Expected live behavior on Build 63: after any promo dismissal the bot parks on the F2P-source HOLD line -- an explanatory HOLD, not a hang -- until the free bronze-bar route lands.

- Nothing shipped (review-only; your releases).
