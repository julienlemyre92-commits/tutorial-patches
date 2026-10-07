# Review note (read-only reviewer) — 2026-10-07 18:04 EDT
## COMBAT OPEN_EQUIPMENT_TAB: Build 130 NO-PROGRESS GUARD logs "HALTING retries" but the retry loop continues — the halt is cosmetic

Follow-up to `2026-10-07-1805-combat-equipment-tab-bar-not-rendered.md`.

### Evidence
- `screenshots/2026-10-07_18-03-55_COMBAT_diag.txt` and `2026-10-07_18-04-55_COMBAT_diag.txt` (runtime Build 418, fresh-character TI run, stage COMBAT, varp281=370).
- The Build 130 NO-PROGRESS GUARD fires **13 times** in the 18-04-55 tail, each with:
  `Build 130 NO-PROGRESS GUARD: EQUIPMENT tab failed 43x..57x with no state change -- HALTING retries. Blocker: check dialogue/widget/tile. Will not retry until state changes.`
- The very next tick after every HALT line is another `COMBAT TICK: OPEN_EQUIPMENT_TAB` → `clickTabIcon EQUIPMENT: FORCED physical click (tutorial prompt, skipping current-tab fast path)` → `NO VERIFIED TARGET` → `counting miss`. The only step seen in the whole file is OPEN_EQUIPMENT_TAB.
- Zero smithing-interface lines, zero dagger progress, zero stage advance in this window; the equipment-tab icon remains unresolvable (all five candidate packed IDs name='' bounds=null).

### Expected vs observed
- Expected: when the no-progress guard decides to HALT, OPEN_EQUIPMENT_TAB attempts should stop until a state change is observed (that is what the message promises).
- Observed: the halt decision is logged but nothing gates the next tick — the FORCED-click path runs regardless, so the guard message is decorative and the 4+ minute miss loop keeps cycling.

### Suggested direction (Alex-owned)
- Make the halt flag actually gate tick entry: check the guard's halt latch before issuing `clickTabIcon EQUIPMENT` (and before the forced-click fast-path skip), not just at the miss-count site. Suggested terminal gate: latch HALT until (a) tab-bar widgets resolve again, or (b) a different stage is entered; plus an escalation after N halted minutes (STATUS dump / alert) so the loop cannot sit silently forever.
- Unchanged from the prior note: the root cause is the hidden tab bar (`Mismatch in overlaid cache archive hash` + worlds lookup failures); the guard bug just removes the only backstop.

### Verification status
- UNVERIFIED pending a fresh diag: after a fix, expect HALT to be followed by silence on the tick (no FORCED physical click lines) until state changes. Fix is only live when NEW runtime lines show it, never the version banner.
