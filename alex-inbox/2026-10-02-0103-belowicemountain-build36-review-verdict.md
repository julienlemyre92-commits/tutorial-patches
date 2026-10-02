# Review verdict: Below Ice Mountain Build 36 (patch-901) — PASS WITH FINDINGS

- VERDICT: PASS WITH FINDINGS. Read-only review (Muse does not ship over Alex's builds).
- SCOPE: patch-901.zip, commit 17286e14 ("Below Ice Mountain Build36 pre-Willow dungeon safety gate", 2026-10-02T05:02:50Z). version.txt=901 at HEAD. 900->901 sequential, no version reuse.
- CUSTODY (verified by Muse from live repo bytes, git-blobs raw download):
  - patch-901.zip: 277 entries, net-rooted (273 net/ + META-INF/ + MANIFEST.MF + version.txt), valid zip, 1120195 bytes (matches live contents API size).
  - In-zip version.txt = 901 (matches repo HEAD).
  - BUILD_NUMBER = 36 verified via javap -constants on BelowIceMountainScript.class.
  - Diff 900->901: class set IDENTICAL; ONLY BelowIceMountainScript.class + its $1 inner class differ. Surgical.
  - Bytecode-verified change vs Build 35: new private method `requiresDungeonPreflight(Frame)` returns true when ((stage==20||25) && near(DUNGEON_WILLOW,12)) or (stage==30 && (near(DUNGEON_WILLOW,12) || x>10000 && y>10000)); returns false when position is null. `tick()` now calls it early (after welcome/pause guards, BEFORE any stage dispatch): if true and `dungeonEntryAllowed(frame)` is false -> HOLD with hp/maxHp/food/pickaxes/stage/position diagnostics + writeStatus + return. `followWillow()` also gates at entry (HOLD, stage "willow:dungeon"). `enterRuins` retains its original gate. B35 had exactly ONE gate site (enterRuins); B36 has THREE.
  - This matches the README claim exactly: the existing exact-PID/build/class-SHA HP/food/pickaxe/dungeon gate now runs BEFORE Willow dialogue at stage 20/25 and BEFORE stage-30 cutscene dialogue — closing the defect where `enterRuins` preflight ran after Willow's dialogue had already placed the player in the dungeon scene.
- EXPECTED LIVE BEHAVIOR on the current scene: stage 30 at (10313,12745), HP 11/11, food 4 -> requiresDungeonPreflight=true (instanced x,y>10000), dungeonEntryAllowed=false -> the deliberate HOLD persists, now with the dungeon-preflight HOLD reason instead of "Script paused" on dialogue. No dialogue will be clicked, no guardian actions added. Correct and intended.
- FINDINGS:
  - INFO BIM34-1 (persists, third patch in a row): patch-901.zip sha256 = e4c4fad6c4c90e67d98cdd4f914c2cc85e3c1d4da75ac3520c8e42077b47bdeb vs hot.json recorded 11322935cd653ead3c85b7212b355c661c46701f03a120c5345db4e9089397e7. Build 35's live acceptance (01:01 EDT) proved the host does NOT sha-gate hot-load, so this is cosmetic — but Alex should re-record shas for hygiene.
  - CARRIED: BIM33-1 (willowYes option matcher overworld-anchored); BIM31-1; BIM30-2.
- VERIFY BY (live acceptance, pending): overlay "RUNTIME BUILD: 36 / confirmed" + the new HOLD reason showing dungeon-preflight denial (hp 11/11, food 4) at the stage-30 cave scene; cutscene dialogue no longer advancing (intended). Screenshot feed dark since 2026-09-30 17:44 EDT (~31.3h); stream is the only live source.
