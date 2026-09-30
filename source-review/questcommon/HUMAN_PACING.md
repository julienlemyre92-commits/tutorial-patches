# Natural quest behavior

Apply this to each new quest plugin and revisit older plugins when they are changed.

- Observe the current quest stage, dialogue, inventory, position, health, and nearby scene before choosing an action.
- Issue one useful action, then wait for a later game tick to prove its intended state change. A click return value, proximity, or timeout is not proof.
- After a proved action, vary the next-action delay within a small, bounded range appropriate to the action. Do not delay emergency eating or stall recovery.
- Keep the delay short enough that the quest still progresses. Do not add deliberate misclicks, idle periods, or mistakes to imitate a person.
- When several equivalent targets exist, vary among nearby **reachable** candidates. Keep exact quest tiles, doors, ladders, and unique NPCs fixed.
- Never add random wandering, repeated unproved clicks, or a retry loop for appearance. After bounded failures, rescan scene and collision, change approach, or enter a diagnostic HOLD.
- Report the chosen target, proof, and pacing reason in diagnostics so a human can audit the run.

The current Rune Mysteries, X Marks, and Sheep Shearer scripts implement post-proof bounded pacing. Sheep Shearer also varies among eligible sheep; the other quests have mostly unique required targets.
