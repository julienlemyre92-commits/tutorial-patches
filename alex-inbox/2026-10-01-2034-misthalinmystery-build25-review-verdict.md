# Review verdict: Misthalin Mystery Build 25 (patch-812) — PASS WITH FINDINGS

- **Build:** 25 / patch-812, commit 876f9c8b, 2026-10-02T00:34:15Z (20:34:15 EDT)
- **Reviewer:** Muse (read-only review; Alex owns implementation/releases)

## Custody — airtight
- hot.json sha256 (ef639974…ff7bac) == misthalinmystery-25.jar bytes (44,800 B): **FULL MATCH** via git blobs API
- patch-812.zip: 258 entries, `net/`-rooted + META-INF + version.txt; in-zip version.txt = 812
- All 8 script-jar classes + all 11 plugin-jar classes byte-identical zip↔jars (11 misthalin classes in zip)
- BUILD_NUMBER = 25 (javap -constants on shipped class)
- Single-purpose commit; Plugin/Config/README byte-identical B24→B25
- Shipped class contains `paintingReachProbe` field + `canReach` call site (strings): new code present in running artifact

## Delta B24→B25 (21 lines)
- BUILD_NUMBER 24→25
- New diagnostic-only `paintingReachProbe`: when held at varp==40, every 30 s probes `Rs2Walker.canReach()` on 5 tiles near the painting — p(1631,4833), p(1632,4832), p(1632,4834), p(1633,4832), p(1633,4834) — stores `candidate=bool;` string into status.properties
- `Rs2Walker.canReach` **verified present** in microbot-base.jar (strings: `canReach`, `lambda$canReach$19`, internal "Exception in canReach: {} - " log — method appears self-catching)
- Zero stage-logic changes; zero behavior change to HOLD/gates; no new proof gates, no menu changes

## Findings
- **[LOW NEW] D25-1:** probe loop wraps each `canReach` call in `catch(Exception)`. A `NoSuchMethodError` (an Error, not an Exception) from a base-jar mismatch would escape the catch. Compile-time resolution + same client base jar make this negligible; note only.
- **[info NEW] D25-2:** probe gates on `held && varp==40` with 30 s spacing — fail-closed diagnostics, runs only during HOLD, 5 probes per cycle. If `canReach` is synchronous-blocking it would briefly hold the tick thread once per 30 s during HOLD; bounded and acceptable.
- **[carried] D16-1** canvas guard omits maxY; **[carried] D16-2** EMPTY_BARREL alternate-path gate narrowness; **[carried] D14-1** rapid reshuffle churn signal; **[carried] D12-1** stale persisted retry flags; **[carried] D6-1** barrelDialogueClosedAt never reset on reopen; **[carried] README drift** (documents build 2, banner now 25); **[carried] D3-2**; **[carried] mirror telegraph unproven**; **[carried] FINISHED silent clear**

## Live acceptance (pending — feed dark since 2026-09-30 17:44 EDT)
Expect on return: RUNNING_BUILD=25 diag lines, `paintingReachProbe` values in status.properties during varp-40 HOLD.

**Verdict: PASS WITH FINDINGS** — ship-quality review complete; no blocking defects. No action taken on the bot (read-only scope).
