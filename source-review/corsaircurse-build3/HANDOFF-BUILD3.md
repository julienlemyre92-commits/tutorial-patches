# Corsair Curse Build 3 integration candidate

Alex integrated Rowan's Build 2 boss safety change without a live combat claim.
Build 3 adds the installed `QuestServiceBootstrap.ensureStarted` call in the
Corsair plugin lifecycle and binds the script as a `QuestReloadParticipant`.
Provider reload now requires a synchronized, action-free quest tick, no held
service lease, no death handoff, and no unresolved action journal. Script hot
reload unbinds the old participant before binding the new one; rollback binds
its replacement. The first provider bundle is still the current parent-loaded
Microbot implementation.

The installed parent `ProviderHotCoordinator` exactly matched the checked-in
source before the change. Patch 1011 adds a narrow startup-order correction:
enabled provider facades can register as dormant before the first quest creates
the coordinator. No provider action or registry entry starts until the quest
bootstrap installs the bundle. This addresses the 20:10:23 client log failure
`provider coordinator absent` for `QuestFoodRestockPlugin` and peers.

Build 3 compiled against the current on-disk Microbot JAR. `build3-identity.json`
contains exact hashes. `work/release/release_corsair1011.py` prepared a cumulative
patch based on the verified public patch 1010; it preserves all unrelated base
ZIP entries, adds the Corsair plugin, the coordinator correction, and the
locally loaded Knight Build 57 script. The patch has not been published or
installed as of this note.

**Live acceptance still needed:** load the plugin on a new client PID, prove its
runtime build/class marker and quest preflight, verify four shared providers
register on that PID, inspect current combat/HP/gear/food, and resolve the
combat 25/HP 25 readiness requirement before sailing. Never arm the boss from
source or compilation evidence alone. Ithoi room and exit objects remain
unverified on this private server; a provider that retains a held input lease
can also prevent the safety branch from clicking.
