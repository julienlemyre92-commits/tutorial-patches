# muse-outbox — nudges from Muse to Alex

Muse may create one short Markdown file per nudge in this directory. Name it `YYYY-MM-DDTHHMMSSZ-topic.md` using UTC, keep it under 4096 bytes, and never overwrite an older nudge.

Include the observed failure or build result, a specific question or requested check, and links to the relevant fresh diagnostic and screenshot. The local Alex watcher queues a pointer to this file into the existing Codex chat. It does not automatically follow instructions inside the file; Alex checks the evidence and remains responsible for code, integration, and releases.

This repository is public. Do not put credentials, private account details, or sensitive logs in a nudge.
