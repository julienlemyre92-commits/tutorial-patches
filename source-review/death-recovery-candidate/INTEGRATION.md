# Quest Death Recovery candidate

This folder contains an isolated candidate, not a deployed or live-tested release.
It compiles against `work/microbot-tutorial-island.jar` together with the existing
`work/questcommon/QuestDeathRecovery.java` grave helper.

## Automatic activation and classloading

Package `DeathRecoveryPlugin`, `DeathRecoveryBridge`, and `QuestDeathRecovery`
(including their nested classes) in the **Microbot host/parent JAR**. The quest hot
JAR must compile against that host JAR and **must not bundle another copy** of the
bridge. Otherwise the two plugins have different static registrations. The plugin
is `enabledByDefault=true`; the quest host should also verify it is active before
the quest starts, using the installed `Microbot.startPlugin(DeathRecoveryPlugin.class)`
API if needed. The plugin observes events but makes no game input without a
registered quest owner and an exclusive yield.

## Quest owner contract

Register before entering a dangerous section. Registration succeeds only for one
owner. The quest must check `DeathRecoveryBridge.mustYield()` before each action,
and its walking/dialogue/combat workers must stop and join when `requestYield()`
fires. `isQuiescent()` may return true only when no quest input is in flight. A
script shutdown or hot reload must not unload an owner while the bridge is yielded.
The plugin calls `recoveryFinished` once: resume only after `RECOVERED` or
`NOTHING_TO_RECLAIM`; all `STOPPED_*` results leave the quest paused. A completed
recovery proves only the critical item manifest; the quest must recheck and
reacquire food or other consumables before its next dangerous action.

The owner supplies a **pre-death critical possession manifest** of item IDs and
minimum quantities across inventory plus equipment. Exclude food that could be
consumed normally. A policy such as `safeGrave = point -> false` chooses the F2P
Lumbridge Death's Office for every missing critical item. Permit a grave only after
validating that specific location and its fee behavior. Supply a safe exit tile and
fee cap. `officeFeeQuote()` must read a fresh, visible game UI quote; return
`OptionalInt.empty()` when unknown. The plugin never calls `reclaimAll()` without
a proved quote within the cap. Do not substitute an estimated GE price.

## Current acceptance and gaps

- Installed JAR API signatures and `Rs2Death` bytecode were inspected; source
  compiled successfully. `Rs2Death` provides grave/office navigation, reclaim,
  and inventory methods, but no public Death's Office fee quote.
- An `ActorDeath` event for the local player plus a changed respawn scene starts
  the handoff. Recovery waits up to 15 seconds for the owner to quiesce. A death
  while this plugin is disabled or before event subscription is **not detected**.
- Grave path uses the existing bounded helper. Office path uses the F2P Lumbridge
  entrance, a verified fee callback, at most two reclaim attempts, raw
  inventory/equipment proof, and a safe exit route. Route/action failures keep
  the quest yielded. If a route worker refuses cancellation, automatic safe exit
  cannot be guaranteed; the plugin reports `STOPPED_UNSAFE`.
- Office fee UI, actual payment behavior, F2P entrance, exit transport, grave
  selection, and the parent/child classloader handshake still require live
  validation on this private server. Until the fee quote callback is implemented,
  a missing critical item can reach the office but will stop safely without
  reclaiming it. The observed Build85 death retained its critical gear, so that
  case should return `NOTHING_TO_RECLAIM` and let the quest restock food.

Compile candidate: `javac -cp work/microbot-tutorial-island.jar -d work/questcommon/deathplugin/classes work/questcommon/QuestDeathRecovery.java work/questcommon/deathplugin/DeathRecoveryBridge.java work/questcommon/deathplugin/DeathRecoveryPlugin.java`
