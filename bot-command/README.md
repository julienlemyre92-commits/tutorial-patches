# bot-command — remote command channel for the Tutorial Island bot (Build 336+)

## Protocol
- Muse writes `bot-command/command.txt`. The plugin polls it ~every 45s via the
  raw GitHub CDN and executes the command at most once.
- Honest latency: the raw CDN can lag ~5-6 min, so a command lands ~1-7 min
  after it's posted. Watch the diag tail for the ack line.

## File format
```
id=<unique id, e.g. 20260929-035500-pause>
ts=<epoch seconds when posted>
cmd=<PAUSE|RESUME|STATUS|RESTART>
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

## Safety rules (enforced by the plugin)
- A command runs at most once: the id is persisted under
  `%USERPROFILE%/.runelite/bot-last-command.txt`.
- Commands older than 15 min (by `ts=`) are ignored, never executed — a stale
  RESTART can never loop the Supervisor.
- Unknown commands are logged and ignored.

## Who writes here
Muse (the bot operator). The file is only writable by repo collaborators, so a
random viewer can't pause Julien's bot.
