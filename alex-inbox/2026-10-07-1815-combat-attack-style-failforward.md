# COMBAT: attack-style lesson fail-forwarded after 30 blind ticks; bot entered the rat pen without the tab lesson (18:13-18:14 EDT)

Read-only review note. Runtime Build 418, fresh-character TI run, stage COMBAT (varp281=370).

## Progress observed (fresh evidence)
`2026-10-07_18-13-29_COMBAT_diag.txt` / `2026-10-07_18-14-50_COMBAT_diag.txt` show the bot moving PAST the equipment-tab stall documented in the 18:04/18:05 notes:
- 18:13:13-18:13:18: `COMBAT TICK: TALK_AFTER_DAGGER`, dialogue continues clicked; "Already have bronze sword -> EQUIP_SWORD_SHIELD"
- 18:13:20: "Wielding bronze sword"; 18:13:22: "Wielding wooden shield"
- 18:13:24: "Combat fast-forward: sword+shield worn -> COMBAT_TAB_LESSON"; `clickTabIcon COMBAT: FORCED physical click` — the COMBAT icon resolved this time (`[164:52 ... bounds=[544,645 33x36]]`, name-matched click at 560,663, MOUSE CLICKED COMBAT tab icon). Note: EQUIPMENT-icon candidates were all null/bounds-null 18:00-18:11 — EQUIP completed without any EQUIPMENT-tab resolution line in the tails, so either the tab bar rendered between 18:11:29 and 18:13:18 or wielding bypassed the tab. Unresolved from tails alone.
- 18:13:26 -> 18:14:26: 31 ticks of "Attack style widget not found yet, retrying" — the attack-style selection widget never appeared.
- 18:14:26: `Build 356: attack-style lookup blind for 30 ticks with tab open -> fail-forward -> ENTER_PEN`
- 18:14:27-18:14:48: `COMBAT TICK: ENTER_PEN`; 18:14:42 "Cache query: click('Open') returned true"; 18:14:48 "Gate snapshot: id=9719 at 3111,9518" -> "Pen gate clicked -> WAIT_GATE_OPEN".

## Review finding
Build 356's fail-forward is by design (no indefinite wait), but it carries the tutorial lesson forward without the lesson being learned: the bot clicked into the rat pen at 18:14:48 with NO observed attack-style selection and no COMBAT-tab content verification. If the combat tab never actually opened (the name-matched click at 560,663 has no verification line), the ENTER_PEN tick is advancing on an unverified state. Watch whether the fight tick finds the attack-style widget; if it re-hits a 30-tick blind window, the lesson is being skipped entirely and the progression gate is hollow.

No action taken (read-only). Suggested verification: look for an attack-style or fight-engagement line after WAIT_GATE_OPEN resolves.
