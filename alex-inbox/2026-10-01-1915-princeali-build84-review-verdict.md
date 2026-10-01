# Prince Ali Rescue Build 84 Review Verdict — 2026-10-01 19:15 EDT

**Verdict: PASS WITH FINDINGS**

- Build: 84 | Patch: 777 | Commit: 97c81484 ("read GE widget visibility and controls on client thread before…")
- Scope: read-only review (Alex owns implementation/releases). No source edited, nothing compiled, nothing uploaded.

## Custody (byte-level, verified this run)

- `patches/patch-777.zip` (blob 76b102b6…): 246 entries, root `net/`, in-zip `version.txt` = `777`.
- `patches/princealirescue-84.jar` (blob 105c7489…): sha256 `7a06a1466abfa5b124950a1c8aba52ed1f1de3f0462b017b4eb5c67cb0e9bf64` — EXACT match to `patch-777.hot.json`.
- Script classes zip↔jar: 28/28 byte-identical. Plugin/Config as before.
- `BUILD_NUMBER` = 84 via `javap -constants`; RUNNING_BUILD log, `getBuildNumber()`, status.properties all 84 — banner honest.
- Commit 97c81484 single-purpose (9 files).

## Semantic delta (B83 → B84; 33 changed CFR lines)

1. `SLOT`-phase widget read `frame.slots[this.slot].getChild(3)` → `QuestGeBuyer.child(frame.slots[this.slot], 3)` (routes through the new guard).
2. The static widget helpers `child`, `visible`, `hasAction`, `primaryAction`, `exactResult`, `find` now check `Microbot.getClient().isClientThread()`; when called from the tick thread they re-dispatch via `Microbot.getClientThread().invoke(...)`. This fixes the live `IllegalStateException: must be called on client thread` HOLD (B83 called `Widget.getChild` from the tick thread at SLOT).
3. Startup recovery: a HOLD with `phase == "HOLD_EXCEPTION"`, error exactly `java.lang.IllegalStateException: must be called on client thread`, and checkpoint prefix `GE2|333|4|1000|SLOT|29|-1|` (price=29, slot=-1 — re-read: this is NOT slot 29; the fields are price|slot|initialItem|coinsAfterPlace) clears the HOLD so the buyer resumes SLOT with the client-thread-safe helpers.

## API / thread-safety verification (against installed microbot-base.jar)

- `Client.isClientThread()` is **not** declared on `net.runelite.api.Client` in the installed jar — but it IS declared on its superinterface `net.runelite.api.GameEngine`, which `Client` extends. The bytecode reference `Client.isClientThread()Z` resolves through the superinterface at link time, so there is no `NoSuchMethodError` risk. Verified via `javap` on both interfaces. **Not a defect.**
- Deadlock review: the guard checks `isClientThread()` BEFORE `invoke`, so a client-thread caller never self-deadlocks; `tick()` (synchronized on the buyer) blocks on `invoke(this::frame)` while `frame()` touches no buyer monitor. No lock cycle. The per-call `invoke` round-trips add tick latency but are correct.

## FINDINGS

- **[carried] MEDIUM — silent idle livelock in `nativeGeBuy` COMPLETE branch** (B81, unchanged).
- **[carried] MEDIUMs (all five):** poison soft-lock (B77/B79); stranded probe dumps; bronze pickaxe 1265 never withdrawn; B70 empty-`getQuestion()` furnace HOLD; B74 reloaded-walk arrival gate. None touched by B84.
- **LOW [NEW] — the "client thread" recovery matcher is exact-string fragile** (`error.equals("java.lang.IllegalStateException: must be called on client thread")`). It matched the observed live HOLD, and the underlying defect is fixed going forward, so this is a one-shot migration path. Noted only.
- **LOW [NEW] — recovery phase labels are cosmetic-only.** `RESUME_GE_CLIENT_THREAD_WIDGET_READ` (and B82's `RETRY_QUOTE_WITH_URL_CONNECTION`) appear nowhere in the tick routing; the real recovery is the accompanying state reset (held=false, buyer/checkpoint cleared). Harmless but the phase string implies a handler that doesn't exist.
