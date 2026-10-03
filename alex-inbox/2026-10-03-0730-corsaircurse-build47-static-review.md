# Corsair Curse Build 47 — static review (Muse, read-only)

- Build marker: version.txt = 1093 (commit c6becc661d, "Corsair Curse Build47 script update", 07:25:30 EDT)
- Diff 46 -> 47 (source-review/corsaircurse-build47 vs build46): 2 hunks only
  1. `BUILD_NUMBER` 46 -> 47
  2. `dialogueOptions()`: added `case 45 -> List.of("I bet I can prove you're well enough to get up.");`
     — same idiom as Build 46's progress==35 override (single forced option)
- Consumer check (dialogue(), line ~831): the forced list is matched against ACTUAL shown
  options (`for expected : allowed, for shown : f.options, norm(shown).equals(norm(expected))`).
  Fail-closed: if the progress==45 menu doesn't show that exact string, it falls through to
  HOLD("Unrecognized dialogue options progress=45 ...") rather than clicking blind. Good.
- The string already exists in the case 35,40 list, so vocabulary is consistent with the
  Gnocci confrontation flow (calling out the faked curse). Switch-order placement after
  case 50 is cosmetic — switch semantics unaffected.
- Packaging: build47-identity.json present; scriptSha256 567afce0... == patch-1093.hot.json
  sha256; fullClassCount 17 / scriptClassCount 14 unchanged; referenceMicrobotSha256 unchanged.
- Verdict: PASS (static). No defects found. Nothing shipped from this loop (Alex owns
  implementation/releases).

Live acceptance IMPOSSIBLE this run: stream removed by uploader ~07:08 EDT (no replacement
URL; not re-flagged), screenshot feed dark since 2026-09-30 17:44 EDT (~65.7h).

Pending next live window: RUNTIME BUILD 47 marker + progress-45 dialogue advancing on the
"I bet I can prove..." option + "Waiting on script toggle" cleared by operator.
Carried watch items: Build 45/46/47 hot-load-vs-restart untested (Build 43 hot-load failed on
the instrumentation-module gap); checkpoint 4/17->1/17 regression unresolved.
