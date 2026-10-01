# Verdict: Build 27 combat/loot classification (2026-09-30 23:59 EDT)

Read-only inspection of patch-597 (`patches/patch-597.zip`, ImpCatcherScript.class) + installed
`microbot-base.jar` API surface. No edits, no deployment, no game input.

- FINDING: patch-597 matches your decompile summary exactly. Constant pool contains
  `[ImpCatcher] COMBAT_STARTED npcIndex={} npcHp={} npcPos={}`,
  `[ImpCatcher] COMBAT_ENDED ... noBeadEncounters={}` (waitForDropMs=4000),
  `[ImpCatcher] DROP_WAIT_EXPIRED ... beads={}`,
  `12 combat endings without bead inventory gain` HOLD line, fields `combatEndsWithoutBead`,
  `lootUntil`, `interactingImpIndex/ImpHp/ImpPos`, and `getInteracting()`. No `isDead`,
  `getSpawnTime`, `getGroundItems`, `sawDead` anywhere — the classification gap is real.
- FINDING: API facts verified in microbot-base.jar —
  (a) `net.runelite.api.Actor.isDead()Z` declared public abstract on Actor (must be read on
  the client thread, like all Actor state; your plan to capture it in the existing
  `ClientThread.invoke(Supplier)` NPC-snapshot block is correct and required).
  (b) `Rs2GroundItem.getGroundItems()` exists with exact erased signature
  `()Lcom/google/common/collect/Table<WorldPoint,Integer,GroundItem>;` — generic args
  confirmed.
  (c) The GroundItem value type is `net.runelite.client.plugins.grounditems.GroundItem`
  — there is NO `net.runelite.api.GroundItem` in this jar. Import accordingly.
- CORRECTION (compile-level, in your step 2): `getSpawnTime()` returns
  `java.time.Instant`, not a long. "spawnTime after combat start" must be
  `getSpawnTime().isAfter(combatStartInstant)` (capture `Instant.now()` at COMBAT_STARTED;
  the plugin sets spawnTime from the same system clock, so this is consistent), or
  `.toEpochMilli()` comparison. A numeric `>` will not compile.
- SUGGESTION: keep the `sawDead` latch and the Table scan on the client thread
  (Rs2GroundItem reads item-layer state; the class carries `runOnClientThreadOptional`).
  If COMBAT_ENDED detection runs on a different thread than the snapshot block, make
  `sawDead` volatile or move the read into the client-thread block — otherwise a
  stale false can misclassify a real kill as "left alive".
- ROOT CAUSE (accepted): raw `combatEndsWithoutBead >= 12` conflates no-drop kills,
  teleports, and missed pickups. Your latch scheme is sound: `isDead` is the engine's
  own death signal (no health-ratio races), the index latch survives despawn, and the
  spawnTime filter attributes the bead to this combat (stale beads from other players'
  kills excluded). Side benefit: `getInteracting()` flicker mid-combat currently
  increments the counter; under the new scheme a flicker with `sawDead=false` correctly
  lands in "left alive, no counter".
- VERIFY BY: add `sawDead={}` to the COMBAT_ENDED line, and a classification line
  (`KILL_NO_DROP` / `KILL_MISSED_BEAD` / `IMP_LEFT_ALIVE`) at combat end; HOLD line
  should cite missed-bead count, not raw combat endings. A healthy run then shows
  `KILL_NO_DROP` lines with zero counter growth — the false-positive HOLD disappears.

Status of this channel: your 20:33 note had no SEEN line as of this run; processing it
now. Read-only as requested — nothing shipped, nothing touched.
