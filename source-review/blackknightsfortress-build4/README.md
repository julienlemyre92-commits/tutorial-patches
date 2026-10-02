# Black Knights' Fortress Microbot candidate

## Build 2 hot handoff (2026-10-02)

Build 4 is the current live candidate on PID40060. Build2 cut a long bank walk after 15 seconds and held at Falador's north gate; Build4 permits 90 seconds of route travel while still stopping after 15 seconds without movement. It also reads `.runelite/blackknightsfortress/control.properties` for exact `enableActions`, PID, build, and script-class SHA approval, so future script-only hot loads do not need RuneLite settings edits. Build4 moved beyond the prior hold position; bank arrival and quest completion remain unverified.

The isolated full-route candidate in [BUILD2_HANDOFF.md](BUILD2_HANDOFF.md) was compiled and loaded through the existing script-only host on unchanged RuneLite PID40060. The live host reports Build2 and the expected main-class hash while the account remains at LOGIN_SCREEN with actions disarmed. Login, quest preflight, bank stock, fortress route, and final completion remain unverified until the current account is observed. The JAR contains only script classes and no manifest. Build1 source is preserved as `BlackKnightsFortressScript.build1.baseline.java`.

Build 1 is a **build-only, untested** RuneLite/Microbot plugin. Alex owns integration, release, and game validation. It has not been installed, deployed, or run in a game client by this task.

## Artifacts

- `BlackKnightsFortress-plugin-1.jar` — plugin host, config, and embedded script. SHA-256 `EDF9E127E7C8462E128687DB90244C4D92B6C9402F001885C42B6955C65C8FFB`.
- `BlackKnightsFortress-script-1.jar` — script classes only, with no manifest, for a **future** hot reload request. SHA-256 `DF4CBD7098D0926B96033B7D93F4ABD2845D1E5849C0B122DE98F70D1BDFD6E5`.

Compiled against `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar` with `javac`: exit 0, two warnings that installed `Rs2GroundItem` is deprecated. No tests or gameplay were run.

## Source and behavior

- [Installed Microbot QuestHelper's BlackKnightFortress route](https://github.com/chsami/Microbot/blob/main/runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/blackknightfortress/BlackKnightFortress.java): varp stages 0 start at Sir Amik, 1 grill, 2 cabbage hole, 3 return to Sir Amik; exact object IDs and floor/zone candidates; 12 QP, helm, chainbody, ordinary cabbage, food and armour recommended.
- [OSRS Wiki quest guide](https://oldschool.runescape.wiki/w/Bkf): F2P quest, 12 QP, iron chainbody, bronze med helm, ordinary cabbage, level-33 knight danger. [OSRS Wiki grill](https://oldschool.runescape.wiki/w/Grill_%28Black_Knights%27_Fortress%29) confirms the Listen-at interaction.
- Installed `VarPlayerID.QP=101`, QuestHelper varplayer 130, `Quest.BLACK_KNIGHTS_FORTRESS`, installed Microbot bank, inventory, shop, dialogue, walker, cache, and player APIs were checked before use. Installed item constants distinguish ordinary cabbage `1965` from Draynor Manor's magic cabbage `1967`; only `1965` is retained, sourced, or used.

The script observes quest state, QP, HP, equipment, inventory, dialogue, bank, and position on each tick. The preflight and status writer run while actions are disabled. Item preparation visits the bank once, deposits nonquest inventory, withdraws gear, cabbage, food, and coins if present, then can buy missing gear at Wayne/Peksa or loot ordinary cabbage at Edgeville Monastery. It equips the disguise before fortress entry. Travel uses worker-thread Microbot walker segments; interactions wait for a later quest, position, floor, inventory, equipment, or dialogue change with three bounded attempts. The plugin has a script-only hot reload host with SHA-256 artifact verification and quiescence at action boundaries; loader requests live at `.runelite/blackknightsfortress-hot`.

## Locked first live check

The plugin's `allowActions` default is false. On the first client load, read `.runelite/blackknightsfortress/status.properties` and the `RUNNING_BUILD` startup log. Confirm live PID, class SHA-256, quest state, varp 130, QP from varp 101, HP/combat, inventory/equipment, and position. If `QuestState.FINISHED`, stop: there is no fresh quest run on this character. If eligible and the scene is safe, set `allowActions=true`, `approvedPid=<live PID>`, `approvedBuild=1`, and `approvedSha256=<script status sha256>` in the plugin config. A restart or new PID requires rearming. Do not use the JAR filename or hot host status as proof that a script class loaded.

## Validation gaps and release cautions

- Current account quest state, 12-QP eligibility, bank stock, supplies, floor path, shop positions/stock, exact door click actions, and private-server dialogue were unavailable while logged out. The initial live check must verify these before actions are armed.
- The route is a candidate translated from QuestHelper zones. It has not proven fortress door sides, ladder landing tiles, guard dialogue, grill varp transition, cabbage-hole cutscene, or the exit route in the private server. A `MOVED` proof on same-plane doors uses an expected destination tile; adjust from fresh scene evidence if the tile differs.
- Low combat (`<15`) or max HP (`<20`) is held before fortress entry. Food requirements are account-relative (five items for weaker accounts, two otherwise). If food is missing from the bank, the script holds; food acquisition is not implemented. Low-health retreat from upper fortress floors also holds, and must be improved before treating an upper-floor run as safe.
- Some Microbot APIs may return from a click before a server state change. The pending proof waits for a later observation and holds after bounded retries. The installed `Rs2GroundItem` API is deprecated, though it compiled; it is called outside the client-thread snapshot to avoid nested client-thread deadlocks.
- Completion requires `QuestState.FINISHED`; logout is requested only in the Falador area with no dialogue. A full uninterrupted run and a second fresh run remain unverified.
