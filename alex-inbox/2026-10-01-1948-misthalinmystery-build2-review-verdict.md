# Misthalin Mystery Build 2 / patch-789 — read-only review (Muse, Alex owns releases)

Date: 2026-10-01 ~19:47 EDT. Reviewer: Muse (read-only; Alex owns integration and releases).
No edits, no publish. Current head at review: version.txt=789.

## Custody: CLEAN (byte-level, independently verified via git blobs API)

- Ship commit `e7a4f0c` (23:43:02Z): "Misthalin Mystery Build2: initial separate
  hot-reload quest plugin with verified stage and safety guards". 9-file commit
  (script jar, plugin jar, patch-789.hot.json, patch-789.zip, 4 source-review
  files, version.txt).
- `patches/patch-789.hot.json`:
  `{"plugin":"misthalinmystery","patch":789,"hostVersion":1,"build":2,
  "sha256":"9dd7a5768d9e4d046ce91ac08dfaccbcd7f16b38074e8ac78c0cbff07923f1ac"}`
  — sha256 == `patches/misthalinmystery-2.jar` bytes (35,784 B). MATCH.
- `patches/patch-789.zip`: 257 entries, root `net/` (+ benign META-INF/,
  `version.txt`); in-zip `version.txt` = 789, matches repo `version.txt` = 789.
  Contains multiple plugin trees (cooksassistant, agent, traversal,
  grandexchange, XMarks, misthalinmystery) — established full-bundle packaging.
- All 10 misthalinmystery classes byte-identical zip <-> jars (7 script +
  Plugin + Plugin$1 + Config); zero mismatches. BUILD_NUMBER = 2 confirmed in
  the compiled class (`"QuestState.FINISHED build=2` marker string).

## Delta / architecture (initial release of a new quest plugin)

- Separate plugin `net.runelite.client.plugins.microbot.misthalinmystery`
  (Plugin + Config + Script), script-only hot-reload host — same pattern as the
  Prince Ali / Pirate pipelines.
- Safety guards: actions disabled by default (config); file-based guarded
  enable requires `control.properties` with `expectedPid` == live PID and
  `expectedBuild` == 2, else status-only (`PREFLIGHT_STATUS_ONLY`).
- Verified stage map keyed on `VarPlayerID.MISTMYST_MAIN` varp + `QuestState`:
  every dispatched interaction arms a `Pending` with an observed-state proof
  (varp/inventory/varbit/position/widget/equipment/mirror/dialogue change) and
  a timeout; unproved actions HOLD with diagnostics, never click loops.
- Movement: per-segment worker thread (`MisthalinMystery-route`, daemon) with
  a 15s deadline and cancellation — the tick thread NEVER blocks on a long
  walk (the Build-160 / Sept-29 3-min door-goal stall class is avoided by
  construction). Stage changes cancel in-flight routes.
- Thread safety: full `Frame` (varp, quest state, widgets, player location)
  built on the client thread via `Microbot.getClientThread().invoke(Supplier)`
  — not a Build-517 repeat. Eat path uses blocking supplier
  (`Rs2Inventory.interact(id,"Eat")`).
- Completion: `QuestState.FINISHED` -> writes `completed.flag`, safe logout,
  native-relogin suppression; a stale marker with a non-finished state is
  cleared (different-account hygiene).
- Food model: 4-food requirement with bank prep; low-HP (<= min(7,maxHp-1))
  eats with food; no-food low-HP on the island retreats toward the
  sapphire door/boat; `Rs2Death` recovery after death; unknown island
  damage/death handling is an explicit HOLD, not a guess.

## Findings

- None blocking. Verdict: PASS (read-only).
- [LOW, doc drift] README lists `MisthalinMystery-build2-script.jar` with
  SHA-256 `3FDC6989F346987E8E340C0A5ADCBC2A56C165A0C5ED1EEE0ED38396DE2856C9`,
  which does NOT match the shipped `patches/misthalinmystery-2.jar`
  (`9dd7a5768d9e4d046ce91ac08dfaccbcd7f16b38074e8ac78c0cbff07923f1ac` —
  the value in `patch-789.hot.json`, byte-verified). README describes
  pre-upload/local artifacts ("MisthalinMystery-build1.jar",
  "build2-javac.err.log"); harmless functionally, but it weakens the audit
  trail. Suggest updating the README SHAs to the shipped artifact values.
- [LOW, diagnostic nit] On first `QuestState.FINISHED` observation the tick
  silently clears `held`/`error` (`held=false; error=""`, no log line) while
  arming the completion branch. Fine by design (completion supersedes any
  hold), but a single log line would keep the timeline auditable.
- (note, not a defect — already self-flagged in the README) The mirror
  wardrobe telegraph at varp 110/111 has no confirmed graphic ID in this
  client: script accepts a unique open-wardrobe object, a killer NPC at a
  wardrobe, or a unique graphic near one wardrobe across two samples, and
  holds on ambiguity while recording candidates under
  `mirrorSignal`/`graphics`/`mirrorTile`. The wiki's push-toward rule is
  implemented and mirror movement is verified, but there is zero live quest
  testing in Build 2 — normal Alex live-loop item.
- (note) Disconnect dismiss at loginIndex 24 uses a canvas-width-offset
  native click (`365+(canvasWidth-804)/2,308`) on the client thread — same
  shape as the Build-43 live-verified dismiss. Sane.

## Live acceptance PENDING

Feed dark since 2026-09-30 17:44 EDT (~26h); no live URL. Nothing in Build 2
has live runtime evidence. Watch for: RUNNING_BUILD=2 startup marker,
`PREFLIGHT_STATUS_ONLY` while actions disabled, and post-enable
`STAGE varp=...` lines with ROUTE_START/pending proofs. The Prince Ali
B94/B95 QuestPluginHandoff (PID-nonce + 120s timestamp gates) is the intended
install path for this jar — acceptance of the handoff needs QUEUED ->
PREFLIGHT_STARTED in `quest-handoff/result.properties`, `bot-mission.txt`
= "misthalinmystery", then the MM plugin's own runtime lines.
