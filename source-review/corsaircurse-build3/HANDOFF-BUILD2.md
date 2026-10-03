# Corsair Curse Build 2 — isolated safety patch

Owner: Rowan. Alex owns integration, deployment and live validation. Nothing in this folder was deployed or used to control the client.

## Artifacts

- `CorsairCurse-script-2.jar`: script-only candidate for the existing hot host.
- `CorsairCurse-plugin-2.jar`: full plugin candidate if a fresh installation is needed.
- `build2-identity.json`: exact JAR, defining-class and compilation-reference hashes.
- `CorsairCurseScript.java`: updated source. Plugin/config source remains Build 1 compatible.

Compiled against `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar`, SHA-256 `b57fadfb9d293847a2df2cc90415bdd503c1be813482cc94b6ee85f8a4b2f7f3`. Compilation succeeded with two existing `QuestRandomEventDismiss` deprecation warnings. No live game test was run.

## Change

At progress 52 in the Ithoi room or while interacting with the combat NPC, an ordinary HOLD or shared-service failure no longer skips the boss safety branch. The branch still requires the exact armed PID/build/class hash, quest mode, exclusive input ownership and no human input. It polls a shared-service result and requests cancellation once if the provider retains an input lease; it will not click until that lease is actually released.

When health is at or below the configured threshold, safety dispatches at most one Eat and verifies a food-count decrease. An unproved Eat is archived and not repeated. It then dispatches at most one Ithoi stair click and accepts escape only when the player is observed on the ground floor in Corsair Cove. An unproved stair click leaves the action journal in place to prevent repetition after reload/restart. Ordinary pending actions are archived before safety input. Shared navigation is refused while a boss interaction or Ithoi room is detected. Runtime status now exposes the safety flags.

## Review and live checks for Alex

1. Verify that the hot host loads `RUNNING_BUILD=2` and defining-class SHA-256 `85618303af22aabeefcd44cef7ec41fb68b76292f6863524aa22ab7d8670ca92` in the actual client PID. File names and copies do not prove load.
2. Test the boss safety branch with fresh HP, inventory, progress and position evidence. Confirm one Eat only, count-change proof, one stair click only and ground-floor arrival proof. If the stair object is absent or the click is unproved, the script holds for manual diagnosis.
3. If a provider retains a HOLD lease, `WAIT_BOSS_PROVIDER_RELEASE` prevents all safety clicks. That provider must yield its lease through its own lifecycle; this patch deliberately does not force-release another provider's input. This remains a live safety limitation.
4. Instance coordinates and the Ithoi stair ID/action remain unverified in this client. A mismapped room or stair will hold. Do not infer successful retreat from a dispatched click.

Build 1's other quest, preparation and validation gaps in `HANDOFF.md` still apply.
