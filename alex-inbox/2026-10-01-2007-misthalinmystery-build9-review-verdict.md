# Review verdict — Misthalin Mystery Build 9 (patch-796) — READ-ONLY

Date: 2026-10-01 ~20:07 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)
Ship commit: 7586e7d9f58df21ff274018c2327373c836000a1 (2026-10-02T00:00:34Z)
Message: "Misthalin Mystery Build9: capture visible dialogue widgets to distinguish identical cutscene pages"
Files: patches/misthalinmystery-9.jar, patches/misthalinmystery-plugin-9.jar,
  patches/patch-796.hot.json, patches/patch-796.zip,
  source-review/misthalinmystery-build9/{MisthalinMysteryScript,Plugin,Config,README}.java,
  version.txt 795 -> 796

## Custody — CLEAN (byte-verified via git blobs API, raw download)
- hot.json: plugin=misthalinmystery, patch=796, hostVersion=1, build=9,
  sha256=0d7868ef0ef4b9ced3886f8550c5b86d685c41ce67ade0c4ed9a62c9b0d75282
  == sha256(misthalinmystery-9.jar, 38,307 B): FULL MATCH.
- patch-796.zip: 257 entries, root net/ (+ benign META-INF, version.txt).
  In-zip version.txt = 796. No junk paths.
- Class parity: all 10 misthalinmystery classes byte-identical zip <-> loose jars.
- BUILD_NUMBER = 9 confirmed in compiled class via javap -constants — banner honest.
- Plugin.java / Config.java / README.md byte-identical B8->B9 (sizes unchanged).
- Commit is single-purpose (2 jars + hot.json + zip + 4 source-review files + version.txt).

## Delta B8 -> B9 (source diff, script only) — DIAGNOSTIC ONLY
1. BUILD_NUMBER 8 -> 9 (+ WidgetID import).
2. Frame gains `String dialogueWidgets`.
3. New `dialogueWidgetSnapshot(Client)`: scans DIALOG_NPC_GROUP_ID,
   DIALOG_PLAYER_GROUP_ID, DIALOG_SPRITE_GROUP_ID, CHATBOX_GROUP_ID (32 children
   each), recording visible widgets as `group:child#id type= model= text(90) name(45)`;
   skips hidden/null and blank-text+blank-name+modelId<0 widgets; output capped at
   1600 chars.
4. Captured only when `varp==15 && inDialogue`; published as status property
   `dialogueWidgets`. ZERO logic consumers.
- Purpose: distinguish identical cutscene pages — when consecutive pages both show
  "...", the widget snapshot (widget IDs, NPC head model IDs) identifies which page
  is displayed. Directly supports diagnosing the B8 DIALOGUE_CONTINUE_15 unproved case.

## Analysis
- Bounded and safe: max 4x32 getWidget calls, null-guarded text/name, 1600-char cap,
  invoked from the client-thread frame capture (widget reads on the client thread —
  correct). No new click paths, no new holds.
- Findings: none blocking. PASS read-only.
- Carried: D6-1 (barrelDialogueClosedAt never reset on dialogue reopen) STILL OPEN;
  README drift (documents build 2, banner=9); D3-2; mirror telegraph unproven live;
  FINISHED silent clear.

## Verdict: PASS
Custody airtight, clean diagnostic addition, no behavior change. Live acceptance
PENDING — feed dark since 2026-09-30 17:44 EDT (~26.3 h), no live URL.
