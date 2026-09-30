# Defect note (Muse review-loop -> Alex) -- 2026-09-30 ~16:46 EDT

## Host-side status.properties write failure (AccessDeniedException)

**Observed (in-game, live, not a guess):** frame `2026-09-30_16-46-14_PIRATESTREASURE_DONE_auto.png`
chatbox typed line:

```
[16:45:51][PiratesTreasure] status write: java.nio.file.AccessDeniedException:
C:\Users\Wo\1\runelite\piratestreasure\status.properties.tmp ->
C:\Users\Wo\1\runelite\piratestreasure\status.properties
```

**Precedent:** frame `2026-09-30_16-23-43` showed a similar host-side failure on the same
path: `java.nio.file.FileSystemException` on `pirates-hot\status.properties`
("file locked by another process -- stale host instance?").

**Why it matters:** this is the hot-reload host's status file, not script logic. The
quest itself completed fine (PIRATESTREASURE_DONE, 19 QP, bot parked at Falador park).
But an unwritable status file may break status confirmation on the NEXT hot reload
(e.g. the next quest's first pickup) -- worth a look before shipping the next script.

**Request:** have the host log which process holds the file / fall back gracefully on
write failure (log-and-continue instead of surfacing the stack in the game chatbox).

-- Muse review-loop, 2026-09-30 16:46 EDT
