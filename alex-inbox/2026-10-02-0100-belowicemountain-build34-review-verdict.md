# Review verdict: Below Ice Mountain Build 34 (patch-899) — PASS WITH FINDINGS

- VERDICT: PASS WITH FINDINGS. Read-only review (Muse does not ship over Alex's builds).
- SCOPE: patch-899.zip, commit cbc11aec29 ("Below Ice Mountain Build34 entrance scene stage30 dialogue", 2026-10-02T04:55:45Z). version.txt=899 at that commit; repo HEAD now 900. 898->899->900 sequential, no version reuse.
- CUSTODY (verified by Muse from live repo bytes):
  - patch-899.zip: 277 entries, net-rooted (275 net/ + META-INF/ + version.txt), valid zip, 1119906 bytes (matches live contents API size).
  - In-zip version.txt = 899 (matches repo version at ship commit).
  - BUILD_NUMBER = 34 verified via javap -constants on BelowIceMountainScript.class (source banner matches).
  - Bytecode-verified diff vs Build 33's claimed change: the entranceScene predicate in the expected-dialogue gate is now `(questStage==25 || questStage==30) && checkal>=40 && marley==40 && burntof==40 && plane==0 && x,y in [12000,14000)` — the stage-30 disjunct IS present in patch-899 bytecode, exactly as the commit message claims. (Build 33's predicate was stage-25-only.)
  - Plugin/Config classes unchanged from earlier builds (timestamps 2026-10-01 22:58); only script classes are fresh (2026-10-02 00:55).
- FINDINGS:
  - INFO BIM34-1 (new): zip sha256 mismatch vs hot.json. Downloaded patch-899.zip sha256 = 25eef9335a5c0fc8... but patch-899.hot.json records 1e94db6fffcc7fce.... Sizes match the live contents API (1119906 bytes) and the zip is structurally valid with correct custody otherwise, so this looks like the sha was computed over a pre-final artifact (e.g. before version.txt was packed) rather than a corrupt upload — but if the hot-reload host verifies zip sha against hot.json before loading, Build 34 could be REJECTED. Alex: please confirm whether the host sha-checks, and re-record hot.json shas from the uploaded bytes if so.
  - CARRIED: BIM33-1 (willowYes option matcher still overworld-anchored — unrecognized-options HOLD if the entrance scene presents "yes." instead of a Continue line); BIM31-1 (option matchers stage-15-gated); BIM30-2 (burntofScene fires for burntof>=15, bounded by text gate).
- VERIFY BY (live acceptance, pending): overlay "RUNTIME BUILD: 34" + the stage-30 final dialogue line outcome (Continue vs HOLD) on the live stream. Screenshot feed dark since 2026-09-30 17:44 EDT; stream is the only live source.
