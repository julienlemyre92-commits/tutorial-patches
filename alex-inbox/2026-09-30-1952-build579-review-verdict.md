# Build 9 (patch-579) review verdict — Muse review-loop, 2026-09-30 19:52 EDT

**Build:** 9 / patch-579.zip (726,507 bytes, zip root `net/` — correct). hot.json: `{"plugin":"impcatcher","patch":579,"hostVersion":1,"build":9,"sha256":"31f7357c0090c5017194fe953bc5003807b5123062621f26872cea0594603efb"}`. Numbering clean (579 = build 9).

**Method:** javap-diffed patch-579 vs patch-578 classes. No source published for builds 7–9 (source-review/ latest is build6-impcatcher), so this is a bytecode review.

**New in Build 9 — native login path.** `tick()` now calls `nativeLoginTick()` first, before the stopped/held checks, and returns early when it handles the login surface:
- Observes a `LoginFrame{state, loginIndex, world, worldTypes, worldList}` via blocking `ClientThread.invoke(Supplier)` (correct overload — returns the value). Null-safe on client.
- LOGGED_IN + welcome screen: `WelcomeScreenEvent.validate()`/`execute()`, max 2 attempts ≥8s apart; after 2 unverified attempts with 12s elapsed → `nativeLoginHeld`, logs `NATIVE_WELCOME_HELD after two unverified attempts; yielding to launcher`.
- LOGIN_SCREEN index 10/34: builds a free-world candidate list (`getTypes().isEmpty()`, `0 <= players < 950`), random pick, `client.changeWorld(world)` on the client thread via blocking `invoke(BooleanSupplier)`, verified next tick, 8s timeout → yield with `NATIVE_LOGIN_YIELD ... launcher fallback`.
- Index 10: `Rs2Keyboard.keyPress(10)` (VK_ENTER = 10) to submit login, max 2 attempts ≥8s apart, then yield.
- Every branch calls `publishLoginStatus(frame, phase, active)`, keeping the STATUS file fresh — this directly addresses the expired-`native_status`/launcher-OCR-misread failure class from the 1812 verdict (status frozen at HOLD, launcher misreading ordinary in-game text as a login screen).
- `tick()` wraps `nativeLoginTick()` in try/catch → on Throwable sets `nativeLoginHeld` and logs `NATIVE_LOGIN_YIELD <throwable>`. Quest logic never runs while login is being handled. Login recovery now also runs while held/stopped (previously the tick halted on HOLD).

**Verdict: no blocking defects.** All client access is on the client thread; retries are bounded and time-gated; every failure mode falls back to the launcher with a clear diag line.

**Nits:**
1. `restoreHotReloadHold()` guard is byte-identical in shape to Build 8 — `(storedBuild >= CURRENT) ? false : CURRENT_STR.equals(requestBuild)`, constant bumped 8→9. The `>=` → `>` change recommended in the 1938 verdict was **not** applied. Whether `RESTORED_HOLD` can fire depends on the hot-reload host writing `~/.runelite/impcatcher-hot/request.properties` with the *incoming* build number — host code is PC-side and nothing in the repo writes that file, so reachability is unverifiable from bytecode alone.
2. `worldSetAttempts` is incremented but never bound-checked (the 8s verify timeout covers it — cosmetic).
3. `keyPress(10)` assumes saved credentials on the login form; if absent, the two bounded attempts fail and it yields to the launcher — acceptable.

**Acceptance triggers (live):** fresh `RUNNING_BUILD=9` line; `NATIVE_LOGIN_*` lines showing a real welcome accept / world select / Play attempt; `RESTORED_HOLD build=9 priorBuild=8` if the restore path fires. **Feed dark since 17:44 EDT (~128 min at filing)** — zero screenshots or diag from the Imp Catcher side, so nothing above is verified live.
