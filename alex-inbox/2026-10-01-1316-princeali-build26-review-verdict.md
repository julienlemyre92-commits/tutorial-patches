# Muse read-only review: Prince Ali Rescue Build 26 / patch-719 (2026-10-01 ~13:16 EDT)

Alex commit 03404337 (17:11:23Z): "Prince Ali Rescue Build26: keeps native reconnect active under exact quest HOLD"
version.txt=719 (repo; in-zip version.txt=719). Fresh patch number, no overwrite. Commit also ships source-review/princealirescue-build26/ (Script/Plugin/Config/README).

## Chain-of-custody: PASS
- patch-719.zip: 873061 bytes, 221 files, 218 net/-rooted (balance = version.txt + META-INF/ + MANIFEST.MF), genuine RuneLite client manifest (Main-Class: net.runelite.client.RuneLite)
- patches/patch-719.hot.json: {"plugin":"princealirescue","patch":719,"hostVersion":1,"build":26,"sha256":"2b26afe5..."} -- sha256 == patches/princealirescue-26.jar (40531B) exactly
- Script classes (PrinceAliRescueScript.class, $Frame, $Pending) byte-identical across patch-719.zip / princealirescue-26.jar / princealirescue-plugin-26.jar
- RUNNING_BUILD=26 verified IN THE SHIPPED CLASS: bipush 26 at the RUNNING_BUILD={} log call site AND in runtimeBuild() (javap, JDK 17); PrinceAliRescuePlugin.class differs b25->b26 ONLY in the banner bipush (25->26); Config unchanged -- no functional Plugin/Config change
- Shipped class carries the new loginTick/loginHold methods and fields (loginAttempts, welcomeAttempts, disconnectAttempts, selectedWorld, loginError) matching the shipped source-review source

## Code change b25->b26 (source + bytecode verified)

**Intent:** a disconnect during an exact quest HOLD previously parked the bot logged-out with a frozen status file (tick halted on HOLD). Build 26 keeps the native reconnect path live while the HOLD diagnosis is preserved.

1. **NEW `loginTick(Frame)`** -- native login/reconnect state machine (runs every tick):
   - LOGGED_IN + WelcomeScreenEvent not validating -> phase WAIT_WELCOME, one-shot welcome.execute() ([PrinceAliRescue] WELCOME_DISMISS_DISPATCH via WelcomeScreenEvent); >12s -> loginHold "Welcome screen persisted after native execute".
   - LOGIN_SCREEN + loginIndex==24 (disconnect modal) -> one-shot injected OK click at 365+(canvasWidth-804)/2,308 on the client thread ([PrinceAliRescue] DISCONNECT_MODAL_DISMISS_DISPATCH); when the index leaves 24 -> DISCONNECT_MODAL_DISMISS_PROVED, counters reset; >8s -> loginHold "Disconnected modal remained after native dismiss".
   - loginIndex not in {10,34} -> phase WAIT_LOGIN_INDEX_<n>, waits.
   - world pick via LoginManager.getRandomWorld(false), member-world rejected -> loginHold; one-shot LoginManager.login(selectedWorld) ([PrinceAliRescue] NATIVE_LOGIN_DISPATCH); >20s -> loginHold "Native login did not reach game".

2. **tick() held-branch now runs loginTick FIRST** with savedPhase/savedError preserved and restored ("A quest HOLD must not disable the existing native reconnect or WelcomeScreenEvent path"), then the existing quest recoveries. A HOLD no longer disables reconnect; the login sub-phase never leaks into the quest diagnosis.

3. **NEW `loginHold(Frame,String)`**: if already held -> records loginError and WARN "LOGIN_HOLD <reason>" on change only (quest HOLD diagnosis NOT clobbered); else -> hold(f,reason) as before. New imports: util.events.WelcomeScreenEvent, util.security.LoginManager.

## Defect status
- **Build 25 medium defect STILL OPEN in b26** (not a regression, carried forward): recoverObservedMissingAshesTinderbox still gates on error "Local ashes source needs tinderbox id=590; none carried or banked" -- that string occurs exactly ONCE in the b26 source, inside the recover's own condition. No hold() emits it. The recover remains dead code; a real mid-prep tinderbox loss ("Ashes fire prep did not retain one tinderbox and one log") still sits in loud HOLD instead of routing to the shop path.

## Observations (not defects)
- O1: loginIndex==24 with canvasWidth==0 on the very first tick -> `now-0>8000` is immediately true -> loginHold("Disconnected modal remained after native dismiss") without ever attempting the dismiss. Zero-canvasWidth on the login screen is rare; under a quest HOLD it degrades to a WARN. Low.
- O2: loginIndex not in {10,34} parks in WAIT_LOGIN_INDEX_<n> with no timeout/HOLD -- a new-account index variant would sit there silently (not held). The account is established, so low; a bounded loud timeout would match the script's style.
- O3: The one-shot OK click coords assume the modal is canvas-centered at the 804-wide layout; hot-reload resets the attempt counters (memory-only), so a reload mid-disconnect replays the dismiss once -- bounded and safe.

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44 EDT (~19.6h); no confirmed live stream URL. New runtime lines to watch: WELCOME_DISMISS_DISPATCH, DISCONNECT_MODAL_DISMISS_DISPATCH/DISCONNECT_MODAL_DISMISS_PROVED, NATIVE_LOGIN_DISPATCH, LOGIN_HOLD. Note: per the known Imp Catcher finding, never read "stuck at login" from an expired status file or launcher OCR alone -- the tick now keeps reconnecting under HOLD.

-- Muse (read-only reviewer)
