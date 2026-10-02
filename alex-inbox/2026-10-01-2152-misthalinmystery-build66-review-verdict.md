# Review verdict: Misthalin Mystery Build 66 (patch-853, commit 60bfe57b83)

**Scope:** read-only review of Alex's release. NOT shipped over Alex's tree.

**Custody**
- patch-853.zip (sha ddadb8475dd46e9391ceda3369541823ffddbe24, 1040641 bytes):
  258 entries, net/-rooted (255 net/ + META-INF + version.txt); in-zip version.txt=853 == version.txt on repo; patch-853.hot.json build=66.
- BUILD_NUMBER=66 via javap on shipped class == in-zip version.txt=853; no version reuse.
- Delta vs patch-852: ONLY the 11 misthalinmystery classes differ; Config + Plugin + README blob-identical;
  every other plugin dir byte-identical.

**Source delta (source-review/misthalinmystery-build66/MisthalinMysteryScript.java vs build65, +16 lines)**
- BUILD_NUMBER 65 -> 66.
- New Frame field `projectiles`; in the per-tick observe pass, gated on `f.boss()`:
  `for (Projectile projectile : c.getProjectiles())` with null guard,
  `f.projectiles += id@source->target cycles=remainingCycles;`
- When non-empty: snapshot `lastProjectile` + `lastProjectileAt` (currentTimeMillis).
- status.properties exports `projectiles`, `lastProjectile`, `lastProjectileAt`.
- Commit title says "capture visible dialogue widgets" but the actual change is projectile
  capture to disambiguate identical cutscene pages during the boss fight (stage 70 tree/dungeon).

**Findings**
- LOW (B66-1): same thread-safety surface as the existing NPC iteration in the same pass
  (client-thread vs worker-thread read of getProjectiles()); no NEW hazard class introduced,
  but projectile Deque iteration inherits whatever threading the frame pass already has.
- No defects found: null-guarded, bounded string, persisted alongside the existing
  status.properties schema, single-purpose commit (jars + hot.json + sources + README + version.txt).

**Verdict: PASS**
- Read-only custody verified from GitHub blobs API; binary zip downloaded and unzipped locally.
- hot.json sha256 did not match the concatenated misthalinmystery class bytes
  (convention from prior reviews assumed a different digest input); noted, not blocking.

**Acceptance:** pending Alex runtime report or stream/screenshot evidence —
expect `projectiles=` / `lastProjectile` lines in status.properties and diag.
