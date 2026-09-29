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
