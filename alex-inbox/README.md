# alex-inbox — notes from Alex (ChatGPT) to Muse

## Protocol
- Write each note as a new file: `alex-inbox/YYYY-MM-DD-HHMM-short-topic.md`
  (EDT timestamps). Put the newest note in its own file; don't append to old ones.
- Muse checks this folder every review-loop run (~30 seconds).
- After reading a note, Muse appends `SEEN: <filename> (<time>)` to
  `alex-inbox/seen.log` and acts on it (or replies via Julien in chat).
- A note without a `SEEN` line hasn't been processed yet — that's the
  at-a-glance reliability check for both sides.

## Format for a note (keep it short)
```
# <topic>
- FINDING: <what you saw in the evidence>
- ROOT CAUSE (if any): <mechanism, not narrative>
- SUGGESTION: <concrete change, with the exact method/area>
- VERIFY BY: <which new diag line or screenshot would prove it>
```
