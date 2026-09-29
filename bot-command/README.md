# bot-command — remote command channel for the bot (Build 336+; both scripts from Build 390+)

## Protocol
- Muse writes `bot-command/command.txt`. The plugin polls it ~every 45s via the
  raw GitHub CDN and executes the command at most once.
- Latency is now low: Julien confirmed 2026-09-29 ~03:55 EDT there is no CDN
  lag anymore (verified: a probe file uploaded to the repo was served fresh
  by the raw CDN immediately). Expect a command to land within ~1-2 min.
  Watch the diag tail for the ack line.

## File format
```
id=<unique id, e.g. 20260929-035500-pause>
ts=<epoch seconds when posted>
cmd=<PAUSE|RESUME|STATUS|RESTART|SWITCH_TO_COOKS|SWITCH_TO_TUTORIAL>
arg=<unused for now>
```

## Commands
- `PAUSE` — freeze all gameplay immediately (screenshots + diag keep flowing,
  so the bot stays observable while paused).
- `RESUME` — resume gameplay.
- `STATUS` — write a state dump (paused, stage, varp281) to the diag log and
  force a screenshot. The diag line is the ack.
- `RESTART` — clean exit; the Supervisor relaunches the game (same mechanism
  as the patch updater).
- `SWITCH_TO_COOKS` (Build 390, Tutorial Island script only) — remote script
  swap WITHOUT a client restart: enables + starts the **Cook's Assistant**
  plugin and stops + disables **Tutorial Island** via the plugin manager,
  flipping the enabled flags so the swap survives restarts. Ack lines:
  `Tutorial Island: SWITCH -- flipping to cooksassistant...` then
  `Tutorial Island: SWITCH COMPLETE -- this plugin stopped and disabled`,
  followed by the Cook's script `Build 390: STARTUP` marker.
- `SWITCH_TO_TUTORIAL` (Build 390, Cook's Assistant script only) — the
  reverse swap, back to **Tutorial Island**.

## Safety rules (enforced by the plugin)
- A command runs at most once: the id is persisted under
  `%USERPROFILE%/.runelite/bot-last-command.txt`.
- Commands older than 15 min (by `ts=`) are ignored, never executed — a stale
  RESTART can never loop the Supervisor.
- Unknown commands are logged and ignored.

## Who writes here
Muse (the bot operator). The file is only writable by repo collaborators, so a
random viewer can't pause Julien's bot.

## Build 391: mission selection (MISSION_SELECT)
- `SWITCH_TO_COOKS` / `SWITCH_TO_TUTORIAL` now also persist the choice to
  `%USERPROFILE%/bot-mission.txt` (`cooks` | `tutorial`), so the selected
  mission survives client restarts without another command.
- On every startup, the FIRST post-login phase in both scripts is
  MISSION_SELECT: the script whose mission matches the file claims the
  scheduler (disables/stops the other plugin, writes
  `%USERPROFILE%/bot-mission-lock.txt`, then a verification tick re-checks
  the lock + overlay state before emitting `SWITCH COMPLETE`); the other
  script yields (enables the desired plugin unless it already holds a fresh
  lock, then disables itself). Quest-state detection runs only after
  `SWITCH COMPLETE`. No quest actions happen before ownership is verified.
- To switch missions, post the SWITCH command (or ask Muse/Alex); toggling
  plugins by hand in the overlay without setting the mission will be
  reverted by the gate on the next tick.
