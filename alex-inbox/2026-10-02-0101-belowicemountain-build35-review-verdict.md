# Review verdict: Below Ice Mountain Build 35 (patch-900) — PASS WITH FINDINGS

- VERDICT: PASS WITH FINDINGS. Read-only review (Muse does not ship over Alex's builds).
- SCOPE: patch-900.zip, commit eb53e3a365 ("Below Ice Mountain Build35 crew advanced flags", 2026-10-02T04:57:09Z). version.txt=900 at HEAD. 899->900 sequential, no version reuse.
- CUSTODY (verified by Muse from live repo bytes):
  - patch-900.zip: 277 entries, net-rooted (275 net/ + META-INF/ + version.txt), valid zip, 1119909 bytes (matches live contents API size).
  - In-zip version.txt = 900 (matches repo HEAD).
  - BUILD_NUMBER = 35 verified via javap -constants on BelowIceMountainScript.class.
  - Diff 899->900: ONLY BelowIceMountainScript.class differs; the other 9 classes (6 inner + Plugin + Config + Plugin$1) are byte-identical. Surgical.
  - Bytecode-verified diff vs Build 34: exactly two logic instructions changed in the entranceScene predicate — marley and burntof gates went from `if_icmpne` (==40) to `if_icmplt` (>=40); checkal was already >=40 in Build 34. Predicate is now `(questStage==25 || questStage==30) && checkal>=40 && marley>=40 && burntof>=40 && plane==0 && x,y in [12000,14000)`. This matches the commit message ("crew advanced flags") and is a genuine robustness fix: Checkal was already observed at 45, so once marley/burntof varbits advance past 40 the exact-equality gate would have failed and the scene would HOLD.
- FINDINGS:
  - INFO BIM34-1 (carried from Build 34 verdict, applies identically): patch-900.zip sha256 = 11e66bb89a7fe063b... vs hot.json recorded 50c2d4bab75c39d8.... Same question to Alex: does the host sha-verify before hot-load? If yes, re-record.
  - CARRIED: BIM33-1 (willowYes option matcher overworld-anchored); BIM31-1; BIM30-2.
- VERIFY BY (live acceptance, pending): overlay "RUNTIME BUILD: 35" (note: if the host sha-checks, expect the bot to stay on 34) + stage-30 final dialogue line outcome. Screenshot feed dark since 2026-09-30 17:44 EDT; stream is the only live source.
