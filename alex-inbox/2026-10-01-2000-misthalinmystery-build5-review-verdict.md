# Review verdict — Misthalin Mystery Build 5 (patch-792) — READ-ONLY

Date: 2026-10-01 ~20:00 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)
Ship commit: cf733cf7e2293122ae456c99160d9a86f05f585a (2026-10-01T23:53:48Z)
Message: "Misthalin Mystery Build5: map observed instance positions objects and routes to quest template coordinates"
Files: patches/misthalinmystery-5.jar, patches/misthalinmystery-plugin-5.jar,
  patches/patch-792.hot.json, patches/patch-792.zip,
  source-review/misthalinmystery-build5/{MisthalinMysteryScript,Plugin,Config,README}.java,
  version.txt 791 -> 792

## Custody — CLEAN (byte-verified via git blobs API, raw download)
- hot.json: plugin=misthalinmystery, patch=792, hostVersion=1, build=5,
  sha256=0148e3bc9679077e7006d07aa95cc99e0b009494b7c1796c7f5e69732f51a669
  == sha256(misthalinmystery-5.jar, 37,073 B): FULL MATCH.
- patch-792.zip: 257 entries, root net/ (+ benign META-INF, version.txt).
  In-zip version.txt = 792. No junk paths.
- Class parity: all 10 misthalinmystery classes byte-identical zip <-> loose jars.
- BUILD_NUMBER = 5 confirmed in compiled class via javap -constants — banner honest.
- Plugin.java / Config.java / README.md byte-identical B4->B5.
- Commit is single-purpose (2 jars + hot.json + zip + 4 source-review files + version.txt).

## Delta B4 -> B5 (source diff, script only)
`f.pos` is now ALWAYS in quest-template coordinates: new `rawPos` holds the live
instance position (`getWorldLocation()`), and `f.pos = instanced ? templatePos : rawPos`.
1. NPC scan: tile = instanced ? fromLocalInstance(npc.getLocalLocation()) : getWorldLocation().
2. Tile-object scan: same mapping via object.getLocalLocation().
3. Ground-item scan: same via g.getLocation().
4. Route walking: walk target resolved on the client thread via new
   `instanceDestination(r.target)` — nearest live position of the template target
   through `toLocalInstance(...).stream().min(by distance to player)`; target absent
   from the current instance -> IllegalStateException("Target absent from current instance: "+target).
5. `visibleObject(id, target)` (nearest-object picker) moved onto the client thread via
   `Microbot.getClientThread().invoke(Supplier)` — the correct blocking pattern —
   comparing objects by template coords in instances; null-guard preserved (`if(tile==null) continue`).
6. New narrow false-hold recovery: clears held when error starts with
   "Unproved SEARCH_BARREL_FIRST after 1 dispatch" AND varp==15 AND instanced AND
   pos!=null AND distance(pos,BARREL)<=3 AND hp==maxHp AND hasContinue AND
   dialogue=="Woo, party on bro!" — logs BARREL_INSTANCE_DIALOGUE_PROVED with
   raw + template positions (B3's TALK_ABIGALE recovery pattern, evidence-gated).
7. Status gains `rawPosition`.

## Analysis
- The IllegalStateException path is SAFE: thrown inside the daemon route worker's
  try/catch -> LOG.warn("route {} segment {}: ...") -> segment ends (r.done=true) ->
  routeTick restarts the segment unconditionally (B3's change), bounded by the
  retained stall guards (20 s no-position-change HOLD, 180 s total-route HOLD,
  10-segment cap). No livelock; diagnosable via the WARN line. A null
  getLocalPlayer() edge funnels through the same bounded path.
- Thread-safety clean: all new client reads go through invoke(Supplier).

## Findings
- None blocking. PASS read-only.
- [LOW/info] D5-1: an instance-absent route target burns segments until the
  10-segment/180 s bound instead of holding immediately — acceptable (bounded,
  logged), noting for the record.
- Carried: README drift (documents build 2, banner=5); D3-2; mirror telegraph
  unproven live; FINISHED silent clear.

## Verdict: PASS
Custody airtight, delta coherent (B4's diagnosis applied: template-space reasoning
end to end), stall bounds preserved. Live acceptance PENDING — feed dark
~26.2 h, no live URL.
