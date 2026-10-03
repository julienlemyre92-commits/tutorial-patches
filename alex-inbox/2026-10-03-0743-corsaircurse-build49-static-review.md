# Corsair Curse Build 49 — static review (Muse, read-only)

- Build marker: version.txt = 1095 (commit a40034043dda, "Corsair Curse Build49 script update", 07:41:00 EDT)
- Diff 48 -> 49 (source-review/corsaircurse-build49 vs build48): 2 hunks only
  1. `BUILD_NUMBER` 48 -> 49
  2. `dialogueOptions()`: added `case 52 -> List.of("I'll be back.");`
     — same forced-option idiom as Build 46's progress==35 and Build 47's progress==45 overrides.
- Consumer check (dialogue(), lines ~831-841): the forced list is matched against ACTUAL
  shown options (`for expected : allowed, for shown : f.options, norm(shown).equals(norm(expected))`).
  Fail-closed: if the progress==52 menu doesn't show exactly "I'll be back.", it falls through
  to `hold("Unrecognized dialogue options progress=52 ...")` rather than clicking blind. Good.
- Vocabulary check: "I'll be back." already appears in the case 50 list, so it is a known
  in-game option string; previously progress 52 fell to default ("Let's go.").
- Packaging: build49-identity.json present; scriptSha256 ad5366aef6... == patch-1095.hot.json
  sha256; fullClassCount 17 / scriptClassCount 14 unchanged; referenceMicrobotSha256 unchanged.
  Version 1095 fresh (never reused); patch-1095.zip + corsaircurse-49.jar in the landing commit.
  (Note: the 07:30 run's transient "1095" contents-API read was a mid-upload race; THIS 1095
  is a real landed commit — confirmed via commits API + trees ground truth.)
- Verdict: PASS (static). No defects found. Nothing shipped from this loop (Alex owns
  implementation/releases).

Live acceptance IMPOSSIBLE this run: last live check 07:36 EDT (new Bumba stream
https://www.youtube.com/watch?v=bWcJJ91v7sA) showed RUNTIME BUILD: BUILD 47 with the bot
parked on "wait armed or input owner" (script toggle still pending operator/Alex); Builds
45/46/47/48/49 all await live observation; screenshot feed dark since 2026-09-30 17:44 EDT.

Pending next live window: RUNTIME BUILD 48/49 markers + hot-load-vs-restart outcome +
progress-45 dialogue advancing (Build 47) + progress-52 "I'll be back." advancing (Build 49)
+ "Waiting on script toggle" cleared + checkpoint 4/17->1/17 regression.
