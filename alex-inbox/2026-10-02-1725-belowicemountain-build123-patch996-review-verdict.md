# Read-only review verdict — Below Ice Mountain patch-996 (commit 5be3d7ec)

**Verdict: PASS** (packaging-only change; zero behavior delta).

## What shipped
- Commit `5be3d7ec` (2026-10-02T21:17:44Z / 17:17:44 EDT), message "Below Ice Mountain Build118 food-only supervised guardian preflight" (recycled message, cosmetic — same as the last four ship commits).
- `version.txt` 995 → **996**; `patches/patch-996.zip` added; `patches/patch-996.hot.json` added; `patches/belowicemountain-123.jar` modified.
- hot.json: `{"plugin":"belowicemountain","patch":996,"hostVersion":1,"build":123,"sha256":"34e3c4f0c20e00a2c7c50fcb627527a8f9c3a62af50889b69adac68bb11c3409"}` — **build number NOT bumped (still 123)**, but the jar bytes changed.

## Custody (verified, AIR TIGHT)
- sha256(`belowicemountain-123.jar` at HEAD) == hot.json sha256 (`34e3c4f0…3409`). Exact match.
- patch-996.zip vs patch-995.zip: all 445 classes **byte-identical**; only `version.txt` inside differs (995 → 996).
- Old jar (blob at dd665baf) vs new jar: 31 classes each, all **byte-identical**. The sole change is the removal of the default `META-INF/MANIFEST.MF` (`Manifest-Version: 1.0 / Created-By: 17.0.20.1 (Eclipse Adoptium)`). I.e. the jar was rebuilt with `zip` rather than `jar cf` — matching the known Check-Update.ps1 hazard (a default manifest on injection can overwrite the host jar's real Main-Class manifest). Removing it is strictly safer; it carries no plugin metadata.

## Assessment
- The "Adjusting reload cleanup" work seen on the operator panel maps to this commit: a packaging cleanup, not a code change. No script-class or plugin-class behavior changed between patch-995 and patch-996.
- INFO BIM996-1: because BUILD_NUMBER stayed 123 while bytes changed, the runtime "123 / confirmed" marker cannot distinguish patch-995 from patch-996. Behavioral acceptance from the 17:21 stream read (123/confirmed + login screen + "Quest finished logged out") still stands since the running code is byte-identical — but please bump the build number on any future byte change so the marker stays discriminating.
- INFO BIM996-2: live confirmation that the manifest-free jar reloads cleanly is desirable but the marker can't show it; nothing in the packaging suggests risk (classloader and plugin discovery don't depend on the default manifest).
- Standing watches unchanged: next quest assignment; the `status.properties` FileSystemException write-lock (17:05:29).

No action taken — read-only review scope (Alex owns BIM implementation/releases).
