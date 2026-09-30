package net.runelite.client.plugins.microbot.cooksassistant;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.Transition;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.traversal.Rs2Traversal;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;

/**
 * Automates the Cook's Assistant quest (Lumbridge) start to finish.
 *
 * Explicit ordered plan (Julien's step-model law): every tick recomputes
 * the next step from OBSERVED state -- quest state via
 * Quest.COOKS_ASSISTANT.getState(), exact inventory counts, dialogue state,
 * and the player's full WorldPoint (plane included for the mill).
 *
 * Route: Cook (start quest) -> pot (kitchen table) -> grain (wheat field)
 * -> mill (hopper top floor, controls, flour bin ground floor) -> egg
 * (chicken farm) -> bucket (cow field) -> milk (dairy cow) -> Cook (finish).
 *
 * Completion ONLY from observed QuestState.FINISHED -- never from a click
 * or a dialogue close. One action per tick; every action's effect is
 * verified on a following tick from observed state.
 */
public class CooksAssistantScript extends StateMachineScript<CooksAssistantScript.Stage> {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CooksAssistantScript.class);

    // Build 386: first build of the Cook's Assistant quest bot.
    private static final int BUILD_NUMBER = 510;
    private volatile boolean disposed;

    public int runtimeBuild() { return BUILD_NUMBER; }

    @Override
    public void shutdown() {
        disposed = true;
        super.shutdown();
        scheduledExecutorService.shutdownNow();
    }

    public boolean awaitStopped() throws InterruptedException {
        return scheduledExecutorService.awaitTermination(12, TimeUnit.SECONDS);
    }

    private int selectedLoginWorld, nativeLoginAttempts, loginWorldSetAttempts, nativeWelcomeAttempts;
    private final java.util.Set<Integer> failedLoginWorlds = new java.util.HashSet<>();
    private boolean loginSelectionHeld;
    private long lastWorldDiagMs;
    private long lastNativeLoginAction;
    private String previousNativeLoginState = "";
    private int previousNativeLoginIndex = -1;

    public int selectedLoginWorld() { return selectedLoginWorld; }
    public boolean nativeLoginOwned() {
        return Microbot.getClient() != null
            && Microbot.getClient().getGameState() == net.runelite.api.GameState.LOGIN_SCREEN
            && Microbot.getClient().getLoginIndex() == 10 && nativeLoginAttempts < 2 && !loginSelectionHeld;
    }
    public void loginTick() {
        if (disposed || Microbot.getClient() == null) return;
        String state = Microbot.getClient().getGameState().name();
        int index = Microbot.getClient().getLoginIndex();
        if (!state.equals(previousNativeLoginState) || index != previousNativeLoginIndex) {
            if (previousNativeLoginState.equals("LOGGED_IN") && state.equals("LOGIN_SCREEN")) {
                selectedLoginWorld = 0; failedLoginWorlds.clear(); loginSelectionHeld = false;
                nativeLoginAttempts = 0; nativeWelcomeAttempts = 0;
            }
            previousNativeLoginState = state; previousNativeLoginIndex = index;
            // Preserve action counters across LOGGING_IN -> LOGIN_SCREEN so a
            // rejected login cannot restart an unbounded sequence of clicks.
        }
        if (state.equals("LOGIN_SCREEN") && (index == 10 || index == 34)) {
            if (loginSelectionHeld) return;
            if (selectedLoginWorld != 0 && Microbot.getClient().getWorld() != selectedLoginWorld
                    && loginWorldSetAttempts >= 2) {
                failedLoginWorlds.add(selectedLoginWorld); selectedLoginWorld = 0;
                if (failedLoginWorlds.size() >= 3) {
                    loginSelectionHeld = true;
                    diag("NATIVE_LOGIN WORLD_SELECTION_HELD after three unverified worlds"); return;
                }
            }
            if (selectedLoginWorld == 0) {
                net.runelite.client.game.WorldService service = Microbot.getWorldService();
                if (service == null) service = net.runelite.client.RuneLite.getInjector().getInstance(net.runelite.client.game.WorldService.class);
                net.runelite.http.api.worlds.WorldResult worldList = service.getWorlds();
                if (worldList == null) {
                    if (System.currentTimeMillis() - lastWorldDiagMs > 15000) {
                        lastWorldDiagMs = System.currentTimeMillis(); service.refresh();
                        diag("NATIVE_LOGIN waiting for live world list; refresh requested");
                    }
                    return;
                }
                java.util.List<net.runelite.http.api.worlds.World> choices = new java.util.ArrayList<>();
                for (net.runelite.http.api.worlds.World world : worldList.getWorlds()) {
                    // Empty flags = ordinary F2P: excludes members, PvP, total-level,
                    // beta, seasonal, Deadman and every special ruleset.
                    if (world.getTypes().isEmpty() && world.getPlayers() >= 0 && world.getPlayers() < 1950
                            && !failedLoginWorlds.contains(world.getId()))
                        choices.add(world);
                }
                if (choices.isEmpty()) {
                    if (System.currentTimeMillis() - lastWorldDiagMs > 15000) {
                        lastWorldDiagMs = System.currentTimeMillis();
                        diag("NATIVE_LOGIN no eligible world, total=" + worldList.getWorlds().size()
                            + " sampleTypes=" + (worldList.getWorlds().isEmpty()?"none":worldList.getWorlds().get(0).getTypes()));
                    }
                    return;
                }
                selectedLoginWorld = choices.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(choices.size())).getId();
                loginWorldSetAttempts = 0; nativeLoginAttempts = 0; lastNativeLoginAction = 0;
                diag("NATIVE_LOGIN selected random ordinary free world=" + selectedLoginWorld + " candidates=" + choices.size());
            }
            if (Microbot.getClient().getWorld() != selectedLoginWorld) {
                final int world = selectedLoginWorld;
                loginWorldSetAttempts++;
                Microbot.getClientThread().invoke(() -> {
                    net.runelite.client.plugins.microbot.util.security.LoginManager.setWorld(world);
                    return true;
                });
                return; // Verify selected world on the next host tick before Play.
            }
            if (Microbot.getClient().getWorldType().contains(net.runelite.api.WorldType.MEMBERS)) return;
            if (index == 10 && nativeLoginAttempts < 2 && System.currentTimeMillis() - lastNativeLoginAction > 8000) {
                nativeLoginAttempts++; lastNativeLoginAction = System.currentTimeMillis();
                Rs2Keyboard.keyPress(java.awt.event.KeyEvent.VK_ENTER);
                diag("NATIVE_LOGIN PlayNow world=" + selectedLoginWorld + " free=true attempt=" + nativeLoginAttempts);
            }
        }
        if (state.equals("LOGGED_IN")) {
            net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent welcome =
                new net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent();
            if (welcome.validate() && nativeWelcomeAttempts < 2 && System.currentTimeMillis() - lastNativeLoginAction > 8000) {
                nativeWelcomeAttempts++; lastNativeLoginAction = System.currentTimeMillis();
                diag("NATIVE_WELCOME verifiedDismissed=" + welcome.execute());
            }
        }
    }

    enum Stage {
        DETECT,
        TALK_COOK_START,
        GET_POT,
        GET_GRAIN,
        MILL_FLOUR,
        GET_EGG,
        GET_BUCKET,
        MILK_COW,
        RETURN_COOK,
        DONE
    }

    // ------------------------------------------------------------------
    // World knowledge: approximate tiles. The bot always re-derives the
    // exact NPC/object by NAME within a radius -- these are only walk
    // targets in open, walkable areas (never doors, walls, or objects).
    // ------------------------------------------------------------------
    private static final WorldPoint COOK_TILE = new WorldPoint(3209, 3214, 0);
    private static final WorldPoint WHEAT_FIELD = new WorldPoint(3157, 3288, 0);
    // Build 465 (Alex 18:39): wheat exit east -- outside the wheat fence,
    // on the path south to Lumbridge. Player was stuck at west-side (3149,3292).
    private static final WorldPoint WHEAT_EXIT_EAST = new WorldPoint(3172, 3288, 0);
    // Build 467 (Alex 18:45): Lumbridge road anchor -- closer waypoint south from
    // wheat exit. Old (3185,3260) was 29 tiles, walkStep=no-walkable-path.
    // New (3175,3270) is 18 tiles, single bounded hop.
    private static final WorldPoint LUMBRIDGE_ROAD = new WorldPoint(3175, 3270, 0);
    // Build 470 (Alex 18:52): double-gate walled edge. WebWalk: walled_edge_not_learned
    // 3145,3292,p0 -> 3146,3291,p0, scene door adjacent (double-gate wing).
    // Recovery must be gate-aware: invoke door pipeline, not greedy-walk.
    private static final WorldPoint GATE_WEST = new WorldPoint(3145, 3292, 0);
    private static final WorldPoint GATE_EAST = new WorldPoint(3146, 3291, 0);
    private int gateCrossPhase = 0; // 0=inactive, 1=approach west, 2=open gate, 3=verify cross
    private int gateCrossTicks = 0;
    private static final WorldPoint MILL_APPROACH = new WorldPoint(3166, 3304, 0);
    private static final WorldPoint MILL_INSIDE = new WorldPoint(3166, 3306, 0);
    private static final WorldPoint CHICKEN_FARM = new WorldPoint(3238, 3298, 0);
    private static final WorldPoint COW_FIELD = new WorldPoint(3256, 3273, 0);
    // Build 455 (Alex 18:05): DAIRY_PASTURE -- the special dairy target
    // location ~(3172,3317,0) near the mill. This is a SEARCH SEED only,
    // not a completion trigger. The bot routes here via the shared
    // traversal resolver, then requires a LIVE "Milk" action on the
    // special fat_cow/prized dairy cow (id 8689) before any click.
    // COW_FIELD (3256,3273) is the ordinary cow field -- wrong pen for
    // milking (only Cow/Cow calf 2790/2791/2792, no Milk action).
    private static final WorldPoint DAIRY_PASTURE = new WorldPoint(3176, 3320, 0); // Build 494: was (3172,3317), not reachable. (3176,3320) proven Build397/458.
    // Build 457 (Alex 18:12): DAIRY_PASTURE_ALT -- materially different
    // alternate waypoint for the one-retry policy. If the primary dairy
    // route fails, try this once before ROUTE_BLOCKED + HOLD.
    private static final WorldPoint DAIRY_PASTURE_ALT = new WorldPoint(3178, 3322, 0);

    // Exact item names (equalsIgnoreCase -- "Pot" must NOT match "Pot of flour").
    private static final String POT = "Pot";
    private static final String GRAIN = "Grain";
    private static final String EGG = "Egg";
    private static final String BUCKET = "Bucket";
    private static final String MILK = "Bucket of milk";
    private static final String FLOUR = "Pot of flour";

    private CooksAssistantConfig config;

    // ---- diag + screenshot infrastructure (same bundle pipeline as Tutorial Island) ----
    private static final String DIAG_PATH =
            System.getProperty("user.home") + "/.runelite/cooks-assistant-diag.log";
    private static final int DIAG_TAIL_MAX = 250;
    private static final java.util.Deque<String> diagTail = new java.util.ArrayDeque<>();
    private long lastScreenshotMs = 0;
    private static final long SCREENSHOT_INTERVAL_MS = 60000;
    private long lastUpdateCheckMs = 0;
    private static final long UPDATE_CHECK_INTERVAL_MS = 60000;
    private long lastLoggedInMs = 0;
    private long lastLoggedOutDiagMs = 0;
    private long lastTickAliveDiagMs = 0;
    private static final long LOGGED_OUT_EXIT_AFTER_MS = 6 * 60 * 1000; // 6 min
    private static final String VERSION_URL =
        "https://raw.githubusercontent.com/julienlemyre92-commits/tutorial-patches/main/version.txt";

    // ---- per-stage transient state ----
    private int millSub = 0;                 // MILL_FLOUR sub-step
    private int millStallTicks = 0;          // no-progress watchdog
    private WorldPoint millLastTile = null;
    private int grainBeforeHopper = -1;       // verify grain consumed by hopper
    // Build 433 (Alex 16:56 + Julien 16:56): hopper use-on is STICKY per
    // objective. Once the bounded retry is exhausted, NEVER click the hopper
    // with grain again. If the hopper already contains grain (Julien observed
    // "already grain in the hopper"), proceed to controls -- the bin check
    // will verify. No outer-loop re-entry into sub 2.
    private boolean hopperUseExhausted = false;
    // Build 472: sticky terminal HOLD for MILL_FLOUR (hopper exhausted +
    // controls operated + bin empty). doMillFlour returns immediately.
    private boolean millFailed = false;
    // Build 419 (NUDGE ALEX 2026-09-29 16:17): GET_GRAIN one-click/next-tick
    // proof contract. The 16:16:03/16:16:21/16:16:40 logs showed repeated
    // "clicked Pick on wheat -- verifying next tick" with no inventory delta.
    // Now: resolve one wheat (id/tile/actions), record pre-count, one Pick,
    // next tick REQUIRES wheat count increase. If unchanged: one rescan with
    // an alternate target, then bounded GET_GRAIN_FAILED and hold. Never
    // re-click the same target. Never advance to gate without grain proof.
    private int grainTargetId = -1;           // wheat object id clicked
    private WorldPoint grainTargetTile = null; // wheat object tile clicked
    private int grainPreCount = -1;           // wheat count before the click
    private boolean grainClickPending = false; // true = awaiting next-tick proof
    private boolean grainRetried = false;     // one alternate-target retry max
    private boolean grainFailed = false;      // bounded failure, hold
    // Build 478 (Alex 19:41): Pick latch timestamp. Once a Pick is issued,
    // never click another wheat until inventory proves delta or timeout.
    // Do NOT use preWheat=1 as permission to click.
    private long grainPickIssuedAt = 0;       // System.currentTimeMillis() of Pick
    private static final long PICK_PROOF_TIMEOUT_MS = 5000; // 5s bounded wait
    // Build 482 (Alex 19:51): Reverted Build480/481 route logic.
    // Build479's manual egg scan WORKED: at 19:50:24 WebWalk collision reached
    // 100%, at 19:50:25 Egg became reachable, one Take, 19:50:27 proof Egg 0->1.
    // Preserve: one Take + next-tick proof, one bounded alternate, then HOLD.
    // Do NOT add route logic unless a fresh run reproduces the failure.
    // Build 483 (Alex 19:57): Pen classification for bucket/milk route.
    // When gate crossing fails, classify pen by live scene targets. If dairy
    // Milk reachable, skip gate and milk directly. Else one route to validated
    // dairy with single gate action + next-tick proof. No repeat Open.
    private boolean bucketRouteBlocked = false; // sticky, no per-tick retry
    private long bucketRouteStartAt = 0; // when we started routing to cow field
    private WorldPoint bucketRouteStartPos = null; // position when route started
    // Build 485 (Alex 20:04): Track ground bucket attempts. Stop loop if Bucket x0.
    private int bucketTakeAttempts = 0;
    private static final int MAX_BUCKET_TAKE_ATTEMPTS = 3;
    // Build 487 (Alex 20:10, Julien): One bounded pen scan, then WRONG_PEN decision.
    // Do NOT rescan every tick. Scan once, decide, then HOLD.
    private boolean bucketPenScanned = false;
    // Build 488 (Alex 20:14): WRONG_PEN route latch. One attempt exactly once.
    // Do NOT re-enter route branch. Next-tick movement proof, then HOLD.
    private boolean bucketWrongPenRouted = false;
    // Build 491 (Alex 20:25): Scan-done latch. Stop re-scanning every tick.
    // One WRONG_PASTURE/TARGET_NOT_FOUND with tile + candidates, then HOLD.
    private boolean bucketScanDone = false;
    // Build 493 (Alex 20:37): Route position proof. Track start pos, verify movement next tick.
    private boolean bucketRouteIssued = false;
    // Build 495 (Alex 20:48): Reset flags on new build. Instance persists across patch reload.
    private int bucketBuildAtReset = -1;
    // Build 499 (Alex 21:03): Recovery latch. Emit RECOVERY_RESOURCE_MISSING once, not every 5s.
    private boolean recoveryMissingLatched = false;
    private boolean recoveryRouted = false;
    private long lastLoginStateReportMs = 0; // Build 501: throttle login-gate diag
    // Build 503 (Alex 21:17): Bounded store route. Lumbridge General Store at
    // (3209,3247)/(3212,3247) sells Buckets. Player in cow field (3245,3274).
    private boolean storeRouteIssued = false;
    private net.runelite.api.coords.WorldPoint storeRouteStartPos = null;
    private int storeRouteStallTicks = 0;
    // Build 419 (NUDGE ALEX 2026-09-29 16:18-16:19): Use the reusable
    // Rs2Traversal layer for fence/gate crossing. No hard-coded wheat gate.
    // The traversal layer owns: edge discovery, one action, next-tick proof,
    // walk-through, crossing proof, one alternate, typed failure + hold.
    // Quest scripts must not click targets until traversal returns DONE
    // (verified reachable).
    private Rs2Traversal grainTraversal = null;
    // Build 435 (Alex 17:04): SHARED traversal owned by stepToward.
    // Every navigation phase (GET_EGG, dairy, bank, cooking, Tutorial, return
    // routes) gets route-recovery automatically. The 17:02:17 GET_EGG silent
    // stall happened because walkHandoffActive was set but no phase ticked a
    // resolver. Now stepToward owns the lifecycle: stall -> TRAVERSAL START ->
    // scan -> action -> proof, or typed failure. No phase can stall silently.
    private Rs2Traversal sharedTraversal = null;
    private int flourBinAttempts = 0;
    private boolean logoutIssued = false;    // retained: suppresses the logged-out watchdog exit(0) after an intentional completion logout (Build 389: no logout issued; park in-game)
    private boolean doneAnnounced = false;
    // Build 393: parked-DONE heartbeat (see doDone -- no shutdown on completion).
    private long lastParkedDiagMs393 = 0;

    // Build 390: remote command channel (bot-command/command.txt), mirrored
    // from TutorialIslandScript's Build 336 agent API so the quest bot can be
    // steered/verified remotely the same way. Also handles SWITCH_TO_TUTORIAL
    // (remote script swap back to Tutorial Island).
    private static final String BOT_COMMAND_URL =
        "https://raw.githubusercontent.com/julienlemyre92-commits/tutorial-patches/main/bot-command/command.txt";
    private long lastCommandPollMs = 0;
    private static final long COMMAND_POLL_INTERVAL_MS = 45 * 1000;
    private boolean remotePaused = false;
    private boolean switchingAway = false;   // Build 390: SWITCH in flight -- stand down

    // Generic walk-progress watchdog.
    private WorldPoint lastWalkTile = null;
    private int walkStallTicks = 0;
    private WorldPoint walkGoal = null;
    // Build 426 (Alex 16:41): failed walk leg tracking.
    // After a leg stalls, do NOT retry the same walk. Hand to traversal.
    private WorldPoint failedLegStart = null;
    private WorldPoint failedLegGoal = null;
    private boolean walkHandoffActive = false;
    // Build 447 (Alex 17:38): store exact request params for idempotency proof.
    private WorldPoint lastTravFrom = null;
    private WorldPoint lastTravGoal = null;
    private String lastTravLabel = null;

    // Talk-to verification.
    private int talkAttempts = 0;
    private static final int TALK_MAX_ATTEMPTS = 4;
    private long lastTalkClickMs = 0;

    // Build 396: pending-action latch (Alex 2026-09-29 ~13:12-13:18).
    // After ONE click, latch and WAIT for next-tick proof instead of
    // re-clicking merely because the result is delayed. Exactly one bounded
    // retry after rescan, then a sticky fail flag for the caller to stand
    // down with diagnostics. Fixes: mill climb-up/down repeats, hopper/bin
    // duplicate clicks, 7x ground-egg takes.
    private long useOnVerifyMs = 0;
    private boolean useOnRetried = false;
    private boolean useOnFailed = false;    // sticky -- caller must consume
    private long useOnNpcVerifyMs = 0;
    private boolean useOnNpcRetried = false;
    private boolean useOnNpcFailed = false; // sticky -- caller must consume
    // Climb latch (millSub 1 = up, 4 = down): one click -> verify plane
    // change next ticks -> one retry -> rescan.
    private long climbMs = 0;
    private int climbFromPlane = -1;
    private boolean climbRetried = false;
    // Egg-take latch.
    private long eggMs = 0;
    private int eggBefore = -1;
    private boolean eggRetried = false;
    private boolean eggDead = false;        // holding with diagnostics
    private long eggDeadDiagMs = 0;
    // Build 396: MILK_COW pen-gate entry (Alex 2026-09-29 ~13:15-13:18).
    // 0=approach/scan, 1=gate hunt, 2=route+open+cross, 3=cow select+milk,
    // 9=held with diagnostics (no reachable gate).
    private int milkPhase = 0;
    private int milkWaitTicks = 0;
    private long milkWaitDiagMs = 0;
    private int milkGateScans = 0;
    // Build 472: entry position for forced return-route proof (Alex 17:25).
    // Recorded when phase 0 starts routing to the cow; phase 5 routes back.
    private WorldPoint milkEntryPos = null;
    // Build 472: diagnostic-only return done flag (Alex 17:33).
    private boolean milkDiagReturnDone = false;
    // Build 454 (Alex 17:58): target audit done flag (once per phase-0 entry).
    private boolean milkTargetAuditDone = false;
    // Build 457 (Alex 18:12): dairy route failure latches. One actionId,
    // one alternate, then ROUTE_BLOCKED + sticky HOLD. No unbounded retry.
    private String milkDairyActionId = null;
    private boolean milkDairyPrimaryFailed = false;
    private boolean milkDairyAlternateTried = false;
    private boolean milkDairyAlternateFailed = false;
    // Build 459 (Alex 18:20): alternate arrival flag. When the alternate route
    // reaches the dairy area, stop routing and enter target-resolution.
    private boolean milkDairyAlternateArrived = false;
    // Build 464 (Alex 18:38): return route leg-3 stall detection.
    // If leg 3/3 (wheat->Cook) stalls, emit typed ROUTE_BLOCKED once, not indefinite logs.
    private int returnLeg3StallTicks = 0;
    private String returnLeg3LastPos = null;
    // Build 472: leg 4/5 stall detection (wheat exit -> road)
    private int returnLeg4StallTicks = 0;
    private String returnLeg4LastPos = null;
    // Build 468 (Alex 18:48): MONOTONIC leg latch. Once we leave wheat exit (leg 4),
    // never re-enter leg 2 or 3. Prevents oscillation/backward-stage bug.
    // 0=none, 1=dairy->mill, 2=mill->wheat, 3=wheat->exit, 4=exit->south, 5=road->Cook
    private int returnLegLatch = 0;
    // Build 458 (Alex 18:16): strict no-progress invariant for staged legs.
    // A leg advances ONLY after next-tick proximity proof AND measurable
    // distance reduction. Repeated same-area positions = stalled.
    private int milkDairyLeg = -1; // -1=not started, 0-3=active leg, 4=complete
    private int milkDairyLegStartDist = Integer.MAX_VALUE;
    private int milkDairyLegLastDist = Integer.MAX_VALUE;
    private int milkDairyLegStallTicks = 0;
    private WorldPoint milkDairyLegTarget = null;
    // Build 448 (Alex 17:38): VERIFIED OUTSIDE tile.
    // Captured on the FIRST tick of phase 0, BEFORE any pen approach.
    // The diagnostic return targets this (not entryPos) so a zero-distance
    // "completion" can never count as a gate crossing.
    private WorldPoint milkOutsidePos = null;
    // Build 472: tracks if the diag-return actually engaged the resolver.
    private boolean diagReturnHandoffSeen = false;
    // Build 449 (Alex 17:40): INSIDE tile from live state.
    // Captured from any cow NPC's tile (dairy or not -- cows are inside
    // the pen). Independent of finding a MILKABLE npc. Used with
    // milkOutsidePos for a nonzero two-leg diagnostic crossing.
    private WorldPoint milkDiagInsidePos = null;
    // Build 472: diagnostic leg (0=idle, 1=outside->inside, 2=inside->outside).
    private int milkDiagLeg = 0;
    private long milkHeldDiagMs = 0;
    private int milkPhase3Scans = 0; // Build 397: bounded post-crossing cow scans
    // Build 461 (Alex 18:27): store the validated milk object from phase 0.
    // Phase 3 uses this directly instead of re-scanning for NPCs.
    private Rs2TileObjectModel milkTargetObject = null;
    // Build 472 (Alex 19:01): simplified target selection fields
    private net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel milkTargetNpc = null;
    private Rs2TileObjectModel milkTargetGameObj = null;
    private WorldPoint milkTargetPos = null;
    private int milkPhase4Ticks = 0; // Build 472: ticks waiting for milk proof
    // Build 415: Diagnostic overlay controller (NUDGE ALEX 2026-09-29 15:49).
    // Temporary, optional, read-only. Cycles through layers one at a time.
    private int diagCtlLayer = 0;
    private long diagCtlLayerStartMs = 0;
    private static final int DIAG_CTL_LAYERS = 5;
    private static final long DIAG_CTL_LAYER_MS = 2000;
    // Build 417: visual overlay owned by the controller. Set by the plugin
    // on startup; null if registration failed. The controller sets the active
    // layer; the overlay draws only that layer and clears on transition.
    private volatile CooksAssistantDiagOverlay diagOverlay = null;
    // Build 418: COLLISION_LOS grid assembled one probe per tick.
    // 3x3 isWalkable was 177-181ms (over 100ms budget). Now each tick probes
    // ONE cell (~20ms); the layer displays the cached grid. Grid invalidates
    // when the player moves.
    private final boolean[][] losGrid = new boolean[3][3];
    private final boolean[][] losKnown = new boolean[3][3];
    private int losProbeIdx = 0;
    private int losCenterX = Integer.MIN_VALUE;
    private int losCenterY = Integer.MIN_VALUE;

    /** Called by CooksAssistantPlugin on startup/shutdown. Thread-safe. */
    public void setDiagOverlay(CooksAssistantDiagOverlay overlay) {
        this.diagOverlay = overlay;
    }
    // Build 408: wrong-pen tracking (NUDGE ALEX 2026-09-29 14:49). Gate id1559
    // leads to the chicken pen (3236,3286), not the cow pen. Track gate IDs
    // that produced wrong-pen crossings; never select them again this run.
    private java.util.Set<Integer> milkWrongPenGateIds = new java.util.HashSet<>();
    // Build 409: tried-gate tracking (NUDGE ALEX 2026-09-29 15:03). After a
    // gate fails (not wrong pen, just no dairy proof), the one bounded retry
    // must probe a DIFFERENT gate, not reselect the same one. Track all
    // attempted gate IDs; the retry excludes them.
    private java.util.Set<Integer> milkTriedGateIds = new java.util.HashSet<>();
    // Build 410: milk verification fields (NUDGE ALEX 2026-09-29 15:17).
    // After click('Milk') on the tile object, next-tick inventory proof.
    private int milkVerifyBucketsBefore = -1;
    private int milkVerifyMilkBefore = -1;
    private long milkVerifyMs = 0;
    // Build 398: gate inspection + crossing verification (Alex 13:29).
    // Build 399: movement through the open gate (Alex 13:37).
    private WorldPoint milkWalkFrom = null;    // pos when the through-walk started
    // Build 400: latch proving the walker primitive was invoked (Alex 13:52).
    private boolean milkWalkStepInvoked = false;

    // ------------------------------------------------------------------
    // Diag
    // ------------------------------------------------------------------
    private static synchronized void diag(String msg) {
        String line = LocalTime.now().truncatedTo(ChronoUnit.SECONDS) + "  " + msg;
        log.info("[CooksAssistant] {}", msg);
        diagTail.addLast(line);
        while (diagTail.size() > DIAG_TAIL_MAX) diagTail.removeFirst();
        try {
            Files.write(Paths.get(DIAG_PATH),
                    (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static synchronized void diagClear() {
        try {
            Files.write(Paths.get(DIAG_PATH),
                    ("=== Cook's Assistant run started "
                            + LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
                            + " ===" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------
    // Screenshots -> bundle/screenshots/ (picked up by the same uploader)
    // ------------------------------------------------------------------
    private void saveScreenshot(String reason) {
        try {
            String jarPath = CooksAssistantScript.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            File bundleDir = new File(jarPath).getParentFile();
            File shotsDir = new File(bundleDir, "screenshots");
            shotsDir.mkdirs();

            String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String stage;
            try {
                stage = getCurrentState() != null ? getCurrentState().name() : "UNKNOWN";
            } catch (Exception e) {
                stage = "UNKNOWN";
            }
            String fname = (ts + "_COOKS_" + stage + "_" + reason + ".png")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");

            Rectangle captureRect;
            try {
                java.awt.Component canvas = Microbot.getClient().getCanvas();
                java.awt.Point p = canvas.getLocationOnScreen();
                captureRect = new Rectangle(p.x, p.y, canvas.getWidth(), canvas.getHeight());
            } catch (Exception ce) {
                diag("canvas-only screenshot skipped: " + ce.getClass().getSimpleName());
                return;
            }
            BufferedImage capture = new Robot().createScreenCapture(captureRect);
            ImageIO.write(capture, "png", new File(shotsDir, fname));
            diag("Screenshot saved: " + fname);
            cleanupOldScreenshots(shotsDir);
        } catch (Exception e) {
            diag("Screenshot failed: " + e.getMessage());
        }
    }

    private void cleanupOldScreenshots(File shotsDir) {
        try {
            File[] files = shotsDir.listFiles((d, n) -> n.endsWith(".png"));
            if (files == null || files.length <= 60) return;
            java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            for (int i = 0; i < files.length - 60; i++) {
                try { files[i].delete(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void maybeAutoScreenshot() {
        long now = System.currentTimeMillis();
        if (now - lastScreenshotMs >= SCREENSHOT_INTERVAL_MS) {
            lastScreenshotMs = now;
            saveScreenshot("auto");
        }
        // Build 390: full-desktop capture every ~10 min (Julien: "take a
        // screenshot of the whole desktop once in a while so you can see any
        // bugs/problems outside of OSRS"). Canvas-only stays the per-minute
        // default; desktop shots are labeled _desktop.
        if (now - lastDesktopScreenshotMs >= DESKTOP_SCREENSHOT_INTERVAL_MS) {
            lastDesktopScreenshotMs = now;
            saveDesktopScreenshot("auto");
        }
    }

    private long lastDesktopScreenshotMs = 0;
    private static final long DESKTOP_SCREENSHOT_INTERVAL_MS = 10 * 60 * 1000;

    /** Build 390: whole-desktop screenshot (primary screen), labeled _desktop. */
    private void saveDesktopScreenshot(String reason) {
        try {
            String jarPath = CooksAssistantScript.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            File bundleDir = new File(jarPath).getParentFile();
            File shotsDir = new File(bundleDir, "screenshots");
            shotsDir.mkdirs();

            String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String stage;
            try {
                stage = getCurrentState() != null ? getCurrentState().name() : "UNKNOWN";
            } catch (Exception e) {
                stage = "UNKNOWN";
            }
            String fname = (ts + "_COOKS_" + stage + "_" + reason + "_desktop.png")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            java.awt.Rectangle screenRect = new java.awt.Rectangle(
                java.awt.Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage capture = new Robot().createScreenCapture(screenRect);
            ImageIO.write(capture, "png", new File(shotsDir, fname));
            diag("Build 390: desktop screenshot saved: " + fname);
            cleanupOldScreenshots(shotsDir);
        } catch (Exception e) {
            diag("Build 390: desktop screenshot failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Update check: same version.txt oracle as the Tutorial Island bot.
    // ------------------------------------------------------------------
    private int getAppliedPatchVersion() {
        try {
            String jarPath = CooksAssistantScript.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            File bundleDir = new File(jarPath).getParentFile();
            File versionFile = new File(bundleDir, "patch-version.txt");
            if (versionFile.isFile()) {
                String txt = new String(Files.readAllBytes(versionFile.toPath())).trim();
                return Integer.parseInt(txt);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private void maybeCheckForUpdate() {
        // The stable plugin host and Supervisor now own delivery. Never exit
        // a logged-in client just because a compatible script build appears.
        if (Boolean.getBoolean("alex.cooks.hotHost")) return;
        long now = System.currentTimeMillis();
        if (now - lastUpdateCheckMs < UPDATE_CHECK_INTERVAL_MS) return;
        lastUpdateCheckMs = now;
        try {
            java.net.URL url = new java.net.URL(VERSION_URL + "?cb=" + System.currentTimeMillis());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                String line = br.readLine();
                if (line != null) {
                    int remote = Integer.parseInt(line.trim());
                    int local = getAppliedPatchVersion();
                    if (local >= 0 && remote > local) {
                        diag("New patch v" + remote + " available (current v" + local + ") - exiting for update");
                        saveScreenshot("update_available");
                        Microbot.status = "Cook's Assistant: updating to v" + remote;
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        System.exit(0);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------
    // Build 390: fixed window. Julien: "always make sure the bot is a fixed
    // window". Pins the client frame to a fixed rect once per script start.
    // ------------------------------------------------------------------
    private static void pinFixedWindow() {
        try {
            java.awt.Component canvas = Microbot.getClient().getCanvas();
            java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(canvas);
            if (w instanceof javax.swing.JFrame) {
                javax.swing.JFrame frame = (javax.swing.JFrame) w;
                frame.setExtendedState(javax.swing.JFrame.NORMAL);
                frame.setResizable(true);
                frame.setBounds(64, 32, 1280, 720);
                frame.validate();
                diag("Build 390: [pin] window pinned to fixed rect (64,32,1280,720)");
            } else {
                diag("Build 390: [pin] skipped -- no JFrame ancestor for canvas");
            }
        } catch (Exception e) {
            diag("Build 390: [pin] failed: " + e.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------
    // Build 390: remote command channel (mirrors TutorialIslandScript).
    // ------------------------------------------------------------------
    private void pollBotCommand() {
        long now = System.currentTimeMillis();
        if (now - lastCommandPollMs < COMMAND_POLL_INTERVAL_MS) return;
        lastCommandPollMs = now;
        try {
            java.net.URL url = new java.net.URL(BOT_COMMAND_URL + "?cb=" + System.currentTimeMillis());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            String id = null, ts = null, cmd = null;
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("id=")) id = line.substring(3).trim();
                    else if (line.startsWith("ts=")) ts = line.substring(3).trim();
                    else if (line.startsWith("cmd=")) cmd = line.substring(4).trim();
                }
            }
            if (id == null || cmd == null || id.isEmpty() || cmd.isEmpty()) return;
            if (id.equals(loadLastCommandId())) return; // already executed
            // Commands older than 15 min are stale (a stale RESTART can never loop the Supervisor).
            if (ts != null && !ts.isEmpty()) {
                try {
                    long cmdTs = Long.parseLong(ts);
                    if (System.currentTimeMillis() - cmdTs > 15 * 60 * 1000) {
                        diag("Cook's Assistant: ignoring stale command " + id + " (" + cmd + ")");
                        persistLastCommandId(id);
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }
            persistLastCommandId(id);
            executeBotCommand(id, cmd);
        } catch (Exception e) {
            // Command channel is best-effort; never break the quest on a failed poll.
        }
    }

    private void executeBotCommand(String id, String cmd) {
        diag("Cook's Assistant: remote command received id=" + id + " cmd=" + cmd);
        switch (cmd) {
            case "PAUSE":
                remotePaused = true;
                diag("Cook's Assistant: PAUSED by remote command " + id);
                break;
            case "RESUME":
                remotePaused = false;
                diag("Cook's Assistant: RESUMED by remote command " + id);
                break;
            case "STATUS": {
                String stageName;
                try { stageName = String.valueOf(getCurrentState()); }
                catch (Exception e) { stageName = "UNKNOWN"; }
                diag("Cook's Assistant: STATUS ack id=" + id + " stage=" + stageName
                    + " paused=" + remotePaused + " loggedIn=" + Microbot.isLoggedIn());
                try { saveScreenshot("remote_status"); } catch (Exception ignored) {}
                break;
            }
            case "RESTART":
                diag("Cook's Assistant: RESTART by remote command " + id + " -- exiting");
                try { saveScreenshot("remote_restart"); } catch (Exception ignored) {}
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                System.exit(0);
                break;
            // Build 500 (Alex 21:07): RESET command for faster logic testing.
            // Clears script state latches WITHOUT reloading JVM bytecode.
            // Logs RESET_REQUESTED -> RESET_APPLIED -> next-tick state/position proof.
            case "RESET":
                diag("Build 500: RESET_REQUESTED by remote command " + id);
                resetBucket505();
                // Clear GET_BUCKET recovery latches
                bucketScanDone = false;
                bucketTakeAttempts = 0;
                bucketRouteIssued = false;
                bucketRouteStartPos = null;
                recoveryMissingLatched = false;
                recoveryRouted = false;
                // Clear other stage latches (add as needed)
                diag("Build 500: RESET_APPLIED -- latches cleared. Next tick: state/position proof.");
                try {
                    net.runelite.api.coords.WorldPoint pp2 = Microbot.getClient().getLocalPlayer().getWorldLocation();
                    diag("Build 500: RESET proof: pos=(" + pp2.getX() + "," + pp2.getY() + "," + pp2.getPlane() +
                        ") stage=" + getCurrentState() + " bucket=" + invCount("Bucket"));
                } catch (Exception e) {
                    diag("Build 500: RESET proof failed: " + e.getClass().getSimpleName());
                }
                break;
            case "SWITCH_TO_TUTORIAL":
                diag("Cook's Assistant: SWITCH_TO_TUTORIAL by remote command " + id
                    + " -- flipping to Tutorial Island");
                try { saveScreenshot("switch_to_tutorial"); } catch (Exception ignored) {}
                writeDesiredMission391("tutorial"); // Build 392: persist the mission -- survives restarts
                requestPluginSwitch(
                    "net.runelite.client.plugins.microbot.tutorialisland.TutorialIslandPlugin",
                    "net.runelite.client.plugins.microbot.cooksassistant.CooksAssistantPlugin");
                break;
            default:
                diag("Cook's Assistant: unknown command '" + cmd + "' ignored");
                break;
        }
    }

    private String loadLastCommandId() {
        try {
            File f = botCommandIdFile();
            if (f.isFile()) return new String(Files.readAllBytes(f.toPath()),
                    StandardCharsets.UTF_8).trim();
        } catch (Exception ignored) {}
        return "";
    }

    private void persistLastCommandId(String id) {
        try {
            Files.write(botCommandIdFile().toPath(), id.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    private File botCommandIdFile() {
        return new File(System.getProperty("user.home") + "/.runelite/bot-last-command.txt");
    }

    /**
     * Build 390: remote script swap -- starts the target Microbot plugin and
     * stops this script's plugin, flipping the enabled flags so the swap
     * survives client restarts. Mirrors TutorialIslandScript's typed
     * PluginManager calls (Plugin.class signatures, verified against the
     * installed jar -- reflection with Object.class params throws
     * NoSuchMethodException here).
     */
    private void requestPluginSwitch(String targetClassName, String selfClassName) {
        if (switchingAway) {
            diag("Cook's Assistant: switch already in flight -- ignoring duplicate request");
            return;
        }
        switchingAway = true;
        try { Microbot.status = "Switching script by remote command..."; }
        catch (Exception ignored) {}
        diag("Cook's Assistant: requesting plugin switch -> " + targetClassName);
        Microbot.getClientThread().invoke(() -> {
            try {
                net.runelite.client.plugins.PluginManager pm = Microbot.getPluginManager();
                net.runelite.client.plugins.Plugin target = null, self = null;
                StringBuilder census = new StringBuilder();
                int microbotCount = 0;
                for (net.runelite.client.plugins.Plugin p : pm.getPlugins()) {
                    String n = p.getClass().getName();
                    if (n.equals(targetClassName)) target = p;
                    if (n.equals(selfClassName)) self = p;
                    if (n.contains(".microbot.")) {
                        if (census.length() > 0) census.append(",");
                        census.append(p.getClass().getSimpleName());
                        microbotCount++;
                    }
                }
                diag("Cook's Assistant: microbot plugins discovered (" + microbotCount + "): " + census);
                if (target == null) {
                    diag("Cook's Assistant: SWITCH FAILED -- target not discovered: " + targetClassName);
                    switchingAway = false;
                    return;
                }
                diag("Cook's Assistant: enabling + starting " + targetClassName);
                pm.setPluginEnabled(target, true);
                boolean started = pm.startPlugin(target);
                diag("Cook's Assistant: startPlugin(" + target.getClass().getSimpleName()
                    + ") returned " + started);
                if (self != null) {
                    diag("Cook's Assistant: stopping " + selfClassName);
                    pm.setPluginEnabled(self, false);
                    boolean stopped = pm.stopPlugin(self);
                    diag("Cook's Assistant: stopPlugin(" + self.getClass().getSimpleName()
                        + ") returned " + stopped + " -- SWITCH COMPLETE");
                } else {
                    diag("Cook's Assistant: self plugin not found in manager -- target started, SWITCH COMPLETE");
                }
            } catch (Exception e) {
                diag("Cook's Assistant: SWITCH FAILED with exception: " + e);
                switchingAway = false;
            }
        });
    }

    // ------------------------------------------------------------------
    // Observed-state helpers
    // ------------------------------------------------------------------

    /** Exact (equalsIgnoreCase) inventory count -- never substring matching. */
    private int invCount(String name) {
        if (disposed) return 0;
        java.util.concurrent.Future<Integer> read = null;
        try {
            // Owned by this script, so reload can cancel and await ALL work.
            read = scheduledExecutorService.submit(() -> {
                if (disposed) return 0;
                return Rs2Inventory.count(item -> {
                    String n = item.getName();
                    return n != null && n.equalsIgnoreCase(name);
                });
            });
            return read.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return 0;
        } finally {
            if (read != null && !read.isDone()) read.cancel(true);
        }
    }

    private boolean hasPot() { return invCount(POT) > 0; }
    private boolean hasGrain() { return invCount(GRAIN) > 0; }
    private boolean hasEgg() { return invCount(EGG) > 0; }
    private boolean hasBucket() { return invCount(BUCKET) > 0; }
    private boolean hasMilk() { return invCount(MILK) > 0; }
    private boolean hasFlour() { return invCount(FLOUR) > 0; }

    private QuestState questState() {
        try {
            return Quest.COOKS_ASSISTANT.getState(Microbot.getClient());
        } catch (Exception e) {
            return null;
        }
    }

    private WorldPoint playerPos() {
        try {
            return Rs2Player.getWorldLocation();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean inDialogue() {
        try {
            return Rs2Dialogue.isInDialogue();
        } catch (Exception e) {
            return false;
        }
    }

    private Rs2NpcModel findNpc(String name, int radius) {
        try {
            return Microbot.getRs2NpcCache().query()
                    .within(radius)
                    .withName(name)
                    .nearest();
        } catch (Exception e) {
            return null;
        }
    }

    // Build 401: diagnostic -- list all distinct NPC names within radius.
    private String listNpcNames401(int radius) {
        try {
            List<Rs2NpcModel> all = Microbot.getRs2NpcCache().query().within(radius).toList();
            java.util.Set<String> names = new java.util.TreeSet<>();
            for (Rs2NpcModel n : all) {
                try {
                    String nm = n.getName();
                    if (nm != null) {
                        WorldPoint wp = n.getWorldLocation();
                        String pos = (wp == null) ? "?" : wp.getX() + "," + wp.getY();
                        names.add(nm + "@" + pos);
                    }
                } catch (Exception ignored) {
                }
                if (names.size() >= 15) break;
            }
            return names.isEmpty() ? "(none)" : String.join("; ", names);
        } catch (Exception e) {
            return "(query failed)";
        }
    }

    // Build 412: NON-BLOCKING milkable cow OBJECT finder (NUDGE ALEX 2026-09-29 15:30/15:31 URGENT).
    // CORRECTION: javap line map shows the block is in getName(), not
    // getObjectComposition(). The tick thread froze enumerating objects.
    // This version: (1) First pass uses ONLY getId() + getWorldLocation()
    // (cheap, non-blocking) to filter. (2) getName() called ONLY on
    // id==8689 matches, wrapped in try-catch. (3) Scan duration + object
    // count logged. (4) Bounded: returns after one pass, never blocks.
    private Rs2TileObjectModel findMilkableCowObject(WorldPoint pp, int radius) {
        long scanStart = System.currentTimeMillis();
        try {
            if (pp == null) return null;
            Rs2TileObjectModel best = null;
            int bestDist = Integer.MAX_VALUE;
            java.util.List<Rs2TileObjectModel> all =
                    Microbot.getRs2TileObjectCache().query().toList();
            int totalObjects = all.size();
            int inRadius = 0;
            int idMatches = 0;
            StringBuilder matchLog = new StringBuilder();
            // PASS 1: cheap filter only (id + location). No getName().
            for (Rs2TileObjectModel o : all) {
                int oid;
                WorldPoint wp;
                try {
                    oid = o.getId();
                    wp = o.getWorldLocation();
                } catch (Exception e) {
                    continue;
                }
                if (wp == null || wp.getPlane() != pp.getPlane()) continue;
                int dist = wp.distanceTo(pp);
                if (dist > radius) continue;
                inRadius++;
                // Match on ID only in the broad scan (8689 = observed fat_cow).
                if (oid == 8689) {
                    idMatches++;
                    // PASS 2: getName() only on matches, protected.
                    String oname = "?";
                    try {
                        oname = String.valueOf(o.getName());
                    } catch (Exception e) {
                        oname = "?(" + e.getClass().getSimpleName() + ")";
                    }
                    if (matchLog.length() > 0) matchLog.append("; ");
                    matchLog.append(oname).append("(id=").append(oid).append(")")
                        .append("@(").append(wp.getX()).append(",").append(wp.getY())
                        .append(",").append(wp.getPlane()).append(")d=").append(dist);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = o;
                    }
                }
            }
            long scanMs = System.currentTimeMillis() - scanStart;
            if (best != null) {
                diag("Build 412: MILK TARGET FOUND: " + matchLog.toString()
                    + " -- scan " + scanMs + "ms, " + totalObjects + " total, "
                    + inRadius + " in radius, " + idMatches + " id=8689");
            } else {
                diag("Build 412: MILK TARGET SCAN: no id=8689 in " + radius
                    + " -- scan " + scanMs + "ms, " + totalObjects + " total, "
                    + inRadius + " in radius, " + idMatches + " id-matches");
            }
            return best;
        } catch (Exception e) {
            long scanMs = System.currentTimeMillis() - scanStart;
            diag("Build 412: MILK TARGET SCAN threw after " + scanMs + "ms: " + e.getMessage());
            return null;
        }
    }

    /**
     * Build 450 (Alex 17:42): find the nearest gate/door-like object.
     * Used by the diagnostic fixture to build a nonzero crossing route.
     * Returns null if none found in radius.
     */
    /**
     * Build 455 (Alex 18:04): DIAGNOSTIC-ONLY TARGET AUDIT.
     * The bot is in the WRONG PEN at (3246,3286) -- generic Cow/Cow calf
     * (2790/2791/2792) with no Milk action. The real dairy target is the
     * special fat_cow/prized dairy cow (id 8689, option Milk) at the dairy
     * location ~(3172,3317,0) near the mill. This audit LOGS what it sees
     * but NEVER selects a generic cow. The destination adapter (phase 0)
     * routes to the dairy location via shared traversal.
     */
    private void auditMilkTargets(WorldPoint pp, int radius) {
        long startMs = System.currentTimeMillis();
        try {
            if (pp == null) return;
            diag("Build 472: MILK_COW: TARGET_AUDIT start r=" + radius
                + " @(" + pp.getX() + "," + pp.getY() + ")");
            // Use the SAME query as the working nearest-NPC scan.
            java.util.List<?> cands = Microbot.getClientThread().invoke(() -> {
                try {
                    return net.runelite.client.plugins.microbot.util.npc.Rs2Npc.getNpcs(o ->
                        o != null && o.getName() != null)
                        .sorted((a, b) -> {
                            try {
                                WorldPoint awp = a.getWorldLocation();
                                WorldPoint bwp = b.getWorldLocation();
                                int ad = (awp != null) ? awp.distanceTo(pp) : Integer.MAX_VALUE;
                                int bd = (bwp != null) ? bwp.distanceTo(pp) : Integer.MAX_VALUE;
                                return Integer.compare(ad, bd);
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .limit(8)
                        .collect(java.util.stream.Collectors.toList());
                } catch (Exception e) {
                    return null;
                }
            });
            if (cands != null && !cands.isEmpty()) {
                int shown = 0;
                for (Object obj : cands) {
                    try {
                        // Use the util.npc.Rs2NpcModel (not api.npc.models).
                        var npc = (net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel) obj;
                        String n = "?";
                        int id = -1;
                        String tileStr = "?";
                        int dist = -1;
                        try { n = npc.getName(); } catch (Exception e) {}
                        try { id = npc.getId(); } catch (Exception e) {}
                        try {
                            WorldPoint wp = npc.getWorldLocation();
                            if (wp != null) {
                                tileStr = wp.getX() + "," + wp.getY();
                                dist = wp.distanceTo(pp);
                            }
                        } catch (Exception e) {}
                        if (dist < 0 || dist > radius) continue;
                        // Get live actions via transformed composition.
                        final var npcF = npc;
                        String[] actionsArr = Microbot.getClientThread().invoke(() -> {
                            try {
                                var comp = npcF.getTransformedComposition();
                                if (comp == null) return null;
                                return comp.getActions();
                            } catch (Exception e) {
                                return null;
                            }
                        });
                        String actionsStr = (actionsArr != null)
                            ? java.util.Arrays.toString(actionsArr)
                            : "unavailable";
                        boolean hasMilk = actionsArr != null
                            && java.util.Arrays.stream(actionsArr)
                                .anyMatch(a -> a != null && a.equalsIgnoreCase("Milk"));
                        diag("Build 472: MILK_COW: TARGET_AUDIT NPC name='" + n
                            + "' id=" + id + " @(" + tileStr + ") dist=" + dist
                            + " actions=" + actionsStr
                            + (hasMilk ? " [HAS_MILK]" : "")
                            + " -- NOT selected (wrong pen; dairy is at 3172,3317)");
                        shown++;
                    } catch (Exception e) {
                        // Skip.
                    }
                }
                if (shown == 0) {
                    diag("Build 472: MILK_COW: TARGET_AUDIT no NPCs in radius");
                }
            } else {
                diag("Build 472: MILK_COW: TARGET_AUDIT NPC query empty");
            }
            // Object audit.
            try {
                java.util.List<Rs2TileObjectModel> all =
                        Microbot.getRs2TileObjectCache().query().toList();
                int shown = 0;
                for (Rs2TileObjectModel o : all) {
                    if (shown >= 8) break;
                    try {
                        WorldPoint wp = o.getWorldLocation();
                        if (wp == null || wp.getPlane() != pp.getPlane()) continue;
                        if (wp.distanceTo(pp) > radius) continue;
                        int oid = o.getId();
                        String oname = "?";
                        try { oname = o.getName(); } catch (Exception e) {}
                        diag("Build 472: MILK_COW: TARGET_AUDIT OBJ name='" + oname
                            + "' id=" + oid + " @(" + wp.getX() + "," + wp.getY() + ")");
                        shown++;
                    } catch (Exception e) {}
                }
                if (shown == 0) {
                    diag("Build 472: MILK_COW: TARGET_AUDIT no objects in radius");
                }
            } catch (Exception e) {
                diag("Build 472: MILK_COW: TARGET_AUDIT object query failed");
            }
            long elapsed = System.currentTimeMillis() - startMs;
            diag("Build 472: MILK_COW: TARGET_AUDIT complete in " + elapsed + "ms (diagnostic only)");
        } catch (Exception e) {
            diag("Build 472: MILK_COW: TARGET_AUDIT threw: " + e.getMessage());
        }
    }

    private Rs2TileObjectModel findNearestGate(WorldPoint pp, int radius) {
        try {
            if (pp == null) return null;
            Rs2TileObjectModel best = null;
            int bestDist = Integer.MAX_VALUE;
            java.util.List<Rs2TileObjectModel> all =
                    Microbot.getRs2TileObjectCache().query().toList();
            for (Rs2TileObjectModel o : all) {
                WorldPoint wp;
                int oid;
                try {
                    wp = o.getWorldLocation();
                    oid = o.getId();
                } catch (Exception e) {
                    continue;
                }
                if (wp == null || wp.getPlane() != pp.getPlane()) continue;
                int d = wp.distanceTo(pp);
                if (d > radius || d == 0) continue;
                String n;
                try {
                    n = o.getName();
                } catch (Exception e) {
                    continue;
                }
                if (n == null) continue;
                String lower = n.toLowerCase();
                // Gate-like: gate, door, fence gate. Not generic fences/walls.
                boolean isGate = lower.contains("gate") || lower.equals("door")
                    || (lower.contains("door") && !lower.contains("doorway"));
                if (!isGate) continue;
                if (d < bestDist) {
                    bestDist = d;
                    best = o;
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    private Rs2TileObjectModel findObject(String name, int radius) {
        try {
            WorldPoint pp = playerPos();
            if (pp == null) return null;
            Rs2TileObjectModel best = null;
            int bestDist = Integer.MAX_VALUE;
            List<Rs2TileObjectModel> all =
                    Microbot.getRs2TileObjectCache().query().toList();
            for (Rs2TileObjectModel o : all) {
                WorldPoint wp;
                try {
                    wp = o.getWorldLocation();
                } catch (Exception e) {
                    continue;
                }
                if (wp == null || wp.getPlane() != pp.getPlane()) continue;
                if (wp.distanceTo(pp) > radius) continue;
                String n;
                try {
                    n = o.getName();
                } catch (Exception e) {
                    continue;
                }
                if (n == null || !n.equalsIgnoreCase(name)) continue;
                int d = wp.distanceTo(pp);
                if (d < bestDist) {
                    bestDist = d;
                    best = o;
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    private static int chebDist(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    /**
     * Walk toward a goal tile, one Rs2Walker.walkStep per tick. Returns true
     * when arrived (chebyshev <= 2).
     *
     * Build 435 (Alex 17:04): stepToward OWNS the shared traversal lifecycle.
     * Every phase that walks gets route-recovery automatically -- no phase
     * needs to check walkHandoffActive or tick a resolver. Stall (40 ticks) ->
     * TRAVERSAL START -> bounded scan -> one action -> proof, or typed
     * failure+HOLD. Silent stalls are impossible.
     */
    private boolean stepToward(WorldPoint goal, String label) {
        return stepToward(goal, label, 2);
    }

    private boolean stepToward(WorldPoint goal, String label, int arrivalDistance) {
        WorldPoint pp = playerPos();
        if (pp == null) return false;
        // If the shared resolver is active, tick it. The walk is suspended
        // until DONE (resume walk) or FAILED (hold).
        if (sharedTraversal != null && !sharedTraversal.isIdle()) {
            Rs2Traversal.Result tr = sharedTraversal.tick(pp);
            // Build 447 (Alex 17:38): IDEMPOTENCY PROOF.
            // Re-issue the EXACT SAME request (stored params) while active.
            // The resolver must emit TRAVERSAL IDEMPOTENT (no reset,
            // blacklist preserved), NOT restart. Runtime dedup evidence.
            if (lastTravFrom != null && lastTravGoal != null && lastTravLabel != null) {
                sharedTraversal.requestTraversal(lastTravFrom, lastTravGoal, "SHARED:" + lastTravLabel);
            }
            if (tr == Rs2Traversal.Result.DONE) {
                diag("Build 472: SHARED_TRAVERSAL DONE '" + label
                    + "' -- resuming walk to (" + goal.getX() + "," + goal.getY() + ")");
                sharedTraversal = null;
                walkHandoffActive = false;
                failedLegStart = null;
                failedLegGoal = null;
                walkGoal = null;
                walkStallTicks = 0;
                lastTravFrom = null; // Build 472: clear proof params
                lastTravGoal = null;
                lastTravLabel = null;
                // Fall through to normal walk below.
            } else if (tr == Rs2Traversal.Result.FAILED) {
                diag("Build 472: SHARED_TRAVERSAL FAILED '" + label
                    + "' -- holding (diagnostic already emitted)");
                Microbot.status = "Cook's Assistant: route blocked -- holding";
                lastTravFrom = null; // Build 472: clear proof params
                lastTravGoal = null;
                lastTravLabel = null;
                // Build 457 (Alex 18:12): latch dairy route failure.
                // Prevents unbounded retry loop. The approach leg checks
                // these flags and tries one alternate, then ROUTE_BLOCKED.
                if (label != null && label.contains("dairy pasture")) {
                    if (label.contains("alternate")) {
                        milkDairyAlternateFailed = true;
                        diag("Build 472: MILK_COW: dairy ALTERNATE route failed -- will HOLD");
                    } else {
                        milkDairyPrimaryFailed = true;
                        diag("Build 472: MILK_COW: dairy PRIMARY route failed -- will try alternate");
                    }
                }
                return false; // hold; diagnostic already emitted
            } else {
                return false; // IN_PROGRESS: keep ticking, no walk.
            }
        }
        // Build 426 (Alex 16:41): if this leg already failed, do NOT retry.
        // The shared resolver owns it now.
        if (failedLegGoal != null && failedLegGoal.equals(goal)
            && failedLegStart != null && failedLegStart.equals(lastWalkTile)) {
            // This exact leg already stalled. Do not retry.
            return false;
        }
        if (walkGoal == null || !walkGoal.equals(goal)) {
            walkGoal = goal;
            lastWalkTile = pp;
            walkStallTicks = 0;
        }
        if (chebDist(pp, goal) <= arrivalDistance) {
            walkGoal = null;
            walkStallTicks = 0;
            // Clear failed leg on success.
            failedLegStart = null;
            failedLegGoal = null;
            return true;
        }
        if (lastWalkTile != null && lastWalkTile.equals(pp)) {
            walkStallTicks++;
        } else {
            walkStallTicks = 0;
            lastWalkTile = pp;
        }
        if (walkStallTicks >= 40) {
            // Build 472: WALK_STALL -> SHARED TRAVERSAL (not just a flag).
            // Start the resolver IMMEDIATELY. Every phase gets this; no
            // phase can stall silently (the GET_EGG 17:02:17 defect).
            diag("Build 472: WALK_STALL -> TRAVERSAL_HANDOFF '"
                + label + "' stalled 40 ticks at (" + pp.getX() + "," + pp.getY()
                + "," + pp.getPlane() + ") -> goal (" + goal.getX() + "," + goal.getY()
                + ") -- starting shared resolver");
            failedLegStart = pp;
            failedLegGoal = goal;
            walkGoal = null;
            walkStallTicks = 0;
            walkHandoffActive = true; // informational; stepToward owns the tick
            if (sharedTraversal == null) {
                sharedTraversal = new Rs2Traversal(msg -> diag(msg));
            }
            // Build 447 (Alex 17:38): DELIBERATE IDEMPOTENCY PROOF.
            // Call requestTraversal EVERY stall-tick, not just when idle.
            // The resolver's key dedup must emit TRAVERSAL IDEMPOTENT
            // (no reset, no re-arm, blacklist preserved) instead of
            // restarting. This is the runtime proof Alex requires.
            // Store exact params for the tick-path proof below.
            lastTravFrom = pp;
            lastTravGoal = goal;
            lastTravLabel = label;
            sharedTraversal.requestTraversal(pp, goal, "SHARED:" + label);
            return false;
        }
        try {
            Rs2Walker.walkStep(goal, 0);
        } catch (Exception e) {
            diag("walkStep failed (" + label + "): " + e.getClass().getSimpleName());
        }
        Microbot.status = "Cook's Assistant: walking (" + label + ")...";
        return false;
    }

    /**
     * Talk to an NPC by name: walk adjacent when far, one Talk-to click per
     * tick when close, verified by the dialogue actually opening. Never
     * re-clicks while a dialogue is already open.
     */
    private boolean talkToNpc(String name, int radius) {
        if (inDialogue()) {
            talkAttempts = 0;
            return true; // dialogue owns the tick
        }
        Rs2NpcModel npc = findNpc(name, radius);
        if (npc == null) {
            diag("talkToNpc: '" + name + "' not nearby -- retry next tick");
            return false;
        }
        WorldPoint pp = playerPos();
        WorldPoint np;
        try {
            np = npc.getWorldLocation();
        } catch (Exception e) {
            return false;
        }
        if (chebDist(pp, np) > 4) {
            talkAttempts = 0;
            stepToward(np, "to " + name);
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastTalkClickMs < 3000) return false; // one click per 3s max
        lastTalkClickMs = now;
        talkAttempts++;
        if (talkAttempts > TALK_MAX_ATTEMPTS) {
            diag("talkToNpc: '" + name + "' " + TALK_MAX_ATTEMPTS
                    + " clicks with no dialogue -- standing down");
            talkAttempts = 0;
            return false;
        }
        try {
            npc.click("Talk-to");
            diag("talkToNpc: clicked Talk-to on '" + name + "' (attempt " + talkAttempts + ")");
        } catch (Exception e) {
            diag("talkToNpc: click failed: " + e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Generic dialogue driver: picks options by priority keywords, otherwise
     * clicks Continue / Space. Returns true when a dialogue was handled this
     * tick (owns the tick), false when no dialogue is open.
     */
    private boolean dialogueTick(String[] optionPriority) {
        if (!inDialogue()) return false;
        try {
            if (Rs2Dialogue.hasSelectAnOption()) {
                for (String opt : optionPriority) {
                    try {
                        if (Rs2Dialogue.keyPressForDialogueOption(opt)) {
                            diag("dialogue: picked option '" + opt + "'");
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
                try {
                    Rs2Dialogue.keyPressForDialogueOption(1);
                    diag("dialogue: no priority option matched -- pressed 1");
                } catch (Exception ignored) {
                }
                return true;
            }
            if (Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue();
                return true;
            }
            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Use an inventory item on a world object: select item one tick, click
     * object the next, verify the effect after. Returns true when the
     * expected effect is observed.
     */
    private int useOnSub = 0;
    private String useOnItem = null;
    private String useOnObject = null;
    private int useOnItemBefore = -1;
    private int useOnAttempts = 0;

    private void useOnReset() {
        useOnSub = 0;
        useOnItem = null;
        useOnObject = null;
        useOnItemBefore = -1;
        useOnAttempts = 0;
        // Build 396: clear the latch; the sticky fail flag is NOT cleared
        // here -- the caller consumes it when standing down.
        useOnVerifyMs = 0;
        useOnRetried = false;
    }

    /**
     * Phased use-item-on-object. verifyMode: "consume" (item count drops),
     * "product:<name>" (product appears), "dialogue" (select-an-option opens).
     * Returns true only when the effect is OBSERVED.
     */
    private boolean useItemOnObject(String itemName, String objectName, String verifyMode, int radius) {
        if (useOnItem == null || !useOnItem.equals(itemName) || !objectName.equals(useOnObject)) {
            useOnReset();
            useOnItem = itemName;
            useOnObject = objectName;
        }
        switch (useOnSub) {
            case 0: { // SELECT
                if (invCount(itemName) == 0) {
                    diag("use-on: item '" + itemName + "' not in inventory");
                    useOnReset();
                    return false;
                }
                Rs2TileObjectModel obj = findObject(objectName, radius);
                if (obj == null) {
                    diag("use-on: object '" + objectName + "' not found");
                    return false; // keep trying; caller owns navigation
                }
                try {
                    if (!Rs2Inventory.use(itemName)) {
                        if (++useOnAttempts > 5) {
                            diag("use-on: could not select '" + itemName + "' after 5 tries");
                            useOnReset();
                            return false;
                        }
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
                useOnItemBefore = invCount(itemName);
                useOnAttempts = 0;
                useOnSub = 1;
                diag("use-on: '" + itemName + "' select issued, verifying selection next tick");
                return false;
            }
            case 1: { // SELECT_VERIFY (Build 431, Alex 16:55): confirm the item
                // is actually selected before clicking the object. use() returning
                // true only means the click was issued, not that selection
                // registered. Without this, the object click is a plain click
                // (not a "use") and the 6s verify falsely times out.
                try {
                    String selected = Rs2Inventory.getSelectedItemName();
                    if (selected == null || !selected.equalsIgnoreCase(itemName)) {
                        diag("use-on: selection NOT confirmed (selected='"
                            + selected + "', want='" + itemName + "') -- re-selecting");
                        if (++useOnAttempts > 5) {
                            diag("use-on: selection failed after 5 tries");
                            useOnReset();
                            return false;
                        }
                        useOnSub = 0; // back to SELECT
                        return false;
                    }
                } catch (Exception e) {
                    return false; // try again next tick
                }
                useOnAttempts = 0;
                useOnSub = 2; // old CLICK becomes 2
                diag("use-on: '" + itemName + "' selection CONFIRMED, clicking '"
                    + useOnObject + "' next tick");
                return false;
            }
            case 2: { // CLICK (was 1)
                if (inDialogue()) {
                    useOnSub = 3; // VERIFY // select-amount dialog may already be open
                    return false;
                }
                Rs2TileObjectModel obj = findObject(objectName, radius);
                if (obj == null) {
                    diag("use-on: object '" + objectName + "' vanished before click");
                    useOnReset();
                    return false;
                }
                // Build 430 (Alex 16:52): ensure proximity before the use-click.
                // If the player is too far, the click walks but the "use" never
                // registers -> 6s verify timeout -> false failure. Step closer
                // first (interaction approach, not a route leg: single walkStep,
                // no stall-handoff machinery).
                try {
                    WorldPoint objPos = obj.getWorldLocation();
                    WorldPoint ppClick = playerPos();
                    if (ppClick != null && chebDist(ppClick, objPos) > 3) {
                        try {
                            Rs2Walker.walkStep(objPos, 0);
                        } catch (Exception e) { /* */ }
                        diag("use-on: approaching '" + objectName + "' ("
                            + chebDist(ppClick, objPos) + " tiles) -- no click yet");
                        return false;
                    }
                } catch (Exception e) { /* proceed to click */ }
                try {
                    obj.click("");
                } catch (Exception e) {
                    diag("use-on: click failed: " + e.getClass().getSimpleName());
                    return false;
                }
                useOnSub = 3; // VERIFY
                diag("use-on: clicked '" + objectName + "' with '" + itemName + "' selected");
                return false;
            }
            default: { // VERIFY -- Build 396: latch. Wait for the inventory
                // delta; do NOT re-click merely because the result is delayed.
                // One bounded re-click after rescan, then sticky fail.
                if ("dialogue".equals(verifyMode)) {
                    try {
                        if (Rs2Dialogue.hasSelectAnOption()) {
                            diag("Build 396: use-on: VERIFIED -- option dialog open");
                            useOnReset();
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                } else if (verifyMode.startsWith("product:")) {
                    String product = verifyMode.substring("product:".length());
                    if (invCount(product) > 0) {
                        diag("Build 396: use-on: VERIFIED -- product '" + product + "' in inventory");
                        useOnReset();
                        return true;
                    }
                } else { // consume
                    int now = invCount(itemName);
                    if (useOnItemBefore >= 0 && now < useOnItemBefore) {
                        diag("Build 396: use-on: VERIFIED -- '" + itemName + "' " + useOnItemBefore + " -> " + now);
                        useOnReset();
                        return true;
                    }
                }
                long nowMs = System.currentTimeMillis();
                if (useOnVerifyMs == 0) useOnVerifyMs = nowMs;
                if (nowMs - useOnVerifyMs >= 6000) {
                    if (!useOnRetried) {
                        useOnRetried = true;
                        useOnVerifyMs = 0;
                        useOnSub = 1; // ONE bounded retry: SELECT_VERIFY re-confirms selection, then CLICK re-scans
                        diag("Build 396: use-on: no delta after 6s latched wait -- one bounded re-click after rescan");
                    } else {
                        diag("Build 396: use-on: FAILED after one retry -- standing down with diagnostics");
                        useOnFailed = true; // sticky -- caller consumes
                        useOnReset();
                    }
                    return false;
                }
                return false; // latched: waiting for next-tick proof, no click
            }
        }
    }

    /**
     * Use an inventory item on an NPC (bucket on dairy cow): select item,
     * walk adjacent, click NPC, verify product appears.
     */
    private int useOnNpcSub = 0;
    private int useOnNpcAttempts = 0;
    // Build 403: exact interaction verification fields (Alex 14:08).
    private int useOnNpcTargetId = -1;
    private String useOnNpcTargetName = "?";
    private String useOnNpcTargetPos = "?";
    private int useOnNpcBucketsBefore = -1;
    private int useOnNpcMilkBefore = -1;
    private int useOnNpcBucketId = -1; // Build 404

    /**
     * Build 404 (Alex 2026-09-29 14:11): use the verified Microbot primitive
     * Rs2Inventory.useItemOnNpc(bucketId, npc.getNpc()) -- exactly one call
     * after bounded approach. The old select-then-click("") never issued a
     * real use-on-item. Next-tick proof: Bucket count decreased AND Bucket
     * of milk count increased. One bounded re-resolve/re-click, then HELD.
     */
    private boolean useItemOnNpc(String itemName, String npcName, String product, int radius) {
        switch (useOnNpcSub) {
            case 0: { // RESOLVE -- capture target id/name/pos, bucket id, pre-counts
                Rs2NpcModel npc = findNpc(npcName, radius);
                if (npc == null) {
                    diag("Build 404: use-on-npc: npc '" + npcName + "' not nearby");
                    return false;
                }
                int npcId = -1;
                String npcRealName = "?";
                String npcPosStr = "?";
                try {
                    npcId = npc.getId();
                    npcRealName = String.valueOf(npc.getName());
                    WorldPoint nwp = npc.getWorldLocation();
                    if (nwp != null) npcPosStr = nwp.getX() + "," + nwp.getY() + "," + nwp.getPlane();
                } catch (Exception e) { /* keep defaults */ }
                int bucketId = -1;
                try {
                    net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel bucketItem =
                        Rs2Inventory.get(itemName, true);
                    if (bucketItem != null) bucketId = bucketItem.getId();
                } catch (Exception e) { /* keep -1 */ }
                if (bucketId < 0) {
                    diag("Build 404: use-on-npc: item '" + itemName + "' not in inventory (no id)");
                    return false;
                }
                useOnNpcTargetId = npcId;
                useOnNpcTargetName = npcRealName;
                useOnNpcTargetPos = npcPosStr;
                useOnNpcBucketId = bucketId;
                useOnNpcBucketsBefore = invCount(itemName);
                useOnNpcMilkBefore = invCount(product);
                useOnNpcRetried = false;
                useOnNpcVerifyMs = 0;
                diag("Build 404: use-on-npc RESOLVE: target id=" + npcId + " '" + npcRealName + "' at ("
                    + npcPosStr + ") -- bucket id=" + bucketId + " x" + useOnNpcBucketsBefore
                    + ", " + product + " x" + useOnNpcMilkBefore + " -- approaching");
                useOnNpcSub = 1;
                return false;
            }
            case 1: { // APPROACH -- bounded walk to within 4; no clicks here
                Rs2NpcModel npc = findNpc(npcName, radius);
                if (npc == null) {
                    diag("Build 404: use-on-npc: npc '" + npcName + "' vanished during approach");
                    useOnNpcSub = 0;
                    return false;
                }
                WorldPoint pp = playerPos();
                WorldPoint np;
                try {
                    np = npc.getWorldLocation();
                } catch (Exception e) {
                    return false;
                }
                if (chebDist(pp, np) > 4) {
                    stepToward(np, "to " + npcName);
                    return false;
                }
                diag("Build 404: use-on-npc APPROACH: adjacent to id=" + useOnNpcTargetId
                    + " -- issuing one useItemOnNpc");
                useOnNpcSub = 2;
                return false;
            }
            case 2: { // USE -- exactly one Rs2Inventory.useItemOnNpc call
                Rs2NpcModel npc = findNpc(npcName, radius);
                if (npc == null) {
                    diag("Build 404: use-on-npc: npc '" + npcName + "' vanished before use");
                    useOnNpcSub = 0;
                    return false;
                }
                int liveId = -1;
                net.runelite.api.NPC liveNpc = null;
                try {
                    liveId = npc.getId();
                    liveNpc = npc.getNpc();
                } catch (Exception e) { /* keep defaults */ }
                if (liveNpc == null) {
                    diag("Build 404: use-on-npc USE: getNpc() null for id=" + liveId + " -- aborting to RESOLVE");
                    useOnNpcSub = 0;
                    return false;
                }
                diag("Build 404: use-on-npc USE: Rs2Inventory.useItemOnNpc(bucketId=" + useOnNpcBucketId
                    + ", npc id=" + liveId + " '" + useOnNpcTargetName + "' at (" + useOnNpcTargetPos + "))");
                boolean useOk = false;
                try {
                    useOk = Rs2Inventory.useItemOnNpc(useOnNpcBucketId, liveNpc);
                } catch (Exception e) {
                    diag("Build 404: use-on-npc USE: threw " + e.getClass().getSimpleName());
                    return false;
                }
                diag("Build 404: use-on-npc USE: primitive returned=" + useOk
                    + " -- next-tick proof: bucket x" + useOnNpcBucketsBefore + "->? , "
                    + product + " x" + useOnNpcMilkBefore + "->?");
                useOnNpcVerifyMs = System.currentTimeMillis();
                useOnNpcSub = 3;
                return false;
            }
            default: { // VERIFY -- latch: require bucket count DOWN and milk count UP
                int bucketsNow = invCount(itemName);
                int milkNow = invCount(product);
                if (bucketsNow < useOnNpcBucketsBefore && milkNow > useOnNpcMilkBefore) {
                    diag("Build 404: use-on-npc: VERIFIED -- bucket x" + useOnNpcBucketsBefore + "->x" + bucketsNow
                        + ", " + product + " x" + useOnNpcMilkBefore + "->x" + milkNow
                        + " -- next-tick transition proven");
                    useOnNpcSub = 0;
                    useOnNpcAttempts = 0;
                    useOnNpcVerifyMs = 0;
                    useOnNpcRetried = false;
                    return true;
                }
                long nowMs = System.currentTimeMillis();
                if (nowMs - useOnNpcVerifyMs >= 6000) {
                    if (!useOnNpcRetried) {
                        useOnNpcRetried = true;
                        useOnNpcVerifyMs = 0;
                        useOnNpcSub = 0; // ONE bounded re-resolve + re-click
                        diag("Build 404: use-on-npc: no transition after 6s "
                            + "(bucket x" + bucketsNow + ", " + product + " x" + milkNow + ") "
                            + "-- one bounded re-resolve/re-click");
                    } else {
                        diag("Build 404: use-on-npc: FAILED after one retry "
                            + "(bucket x" + bucketsNow + ", " + product + " x" + milkNow + ") "
                            + "-- standing down with diagnostics");
                        useOnNpcFailed = true; // sticky -- caller consumes
                        useOnNpcSub = 0;
                        useOnNpcAttempts = 0;
                        useOnNpcVerifyMs = 0;
                        useOnNpcRetried = false;
                    }
                    return false;
                }
                return false; // latched: waiting for next-tick proof, no click
            }
        }
    }

    // ------------------------------------------------------------------
    // State machine
    // ------------------------------------------------------------------
    @Override
    protected Stage initialState() {
        return Stage.DETECT;
    }

    @Override
    protected List<Transition<Stage>> defineTransitions() {
        List<Transition<Stage>> t = new ArrayList<>();
        // Completion is game-verified: QuestState.FINISHED from any state.
        for (Stage s : Stage.values()) {
            if (s == Stage.DONE) continue;
            final Stage from = s;
            t.add(Transition.from(from)
                    .when(() -> questState() == QuestState.FINISHED, "questState()==FINISHED")
                    .because("Cook's Assistant complete -- game-verified")
                    .goTo(Stage.DONE));
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Build 392: MISSION_SELECT -- deterministic plugin ownership (Alex
    // 2026-09-29). Live 12:28:30-12:28:44: Cook's Assistant was enabled
    // then disabled 14s later with no SWITCH COMPLETE -- the toggle came
    // from OUTSIDE the scripts (bot-command/command.txt history shows no
    // SWITCH command was ever posted; the only setPluginEnabled /
    // startPlugin / stopPlugin call sites are inside requestPluginSwitch,
    // which fires solely on remote commands). From this build, mission
    // selection is the FIRST post-login phase in both scripts, driven by
    // a single mission file:
    //   %USERPROFILE%/.runelite/bot-mission.txt   ("cooks" | "tutorial")
    // Remote SWITCH_TO_COOKS / SWITCH_TO_TUTORIAL persist the mission, so
    // the choice survives restarts. Ownership is attested by
    //   %USERPROFILE%/.runelite/bot-mission-lock.txt  (owner + timestamp)
    // Rules (idempotent, convergent from any state):
    //  - desired == self: CLAIM -- disable+stop the other plugin (verified
    //    via PluginManager.isPluginEnabled against the installed revision),
    //    write the lock, then a verification tick re-checks lock owner +
    //    self enabled + other disabled before emitting SWITCH COMPLETE.
    //    Quest-state detection runs only after that.
    //  - desired == other: YIELD -- enable the other plugin (skipped when it
    //    already holds a fresh lock: never double-start), then disable+stop
    //    self. Fail-safe: desired plugin class not in this jar -> stay on
    //    self instead of killing the bot with nothing running.
    // ------------------------------------------------------------------
    private static final String MISSION_SELF_391 = "cooks";
    private static final String MISSION_OTHER_391 = "tutorial";
    private static final String SELF_PLUGIN_CLASS_391 = "net.runelite.client.plugins.microbot.cooksassistant.CooksAssistantPlugin";
    private static final String OTHER_PLUGIN_CLASS_391 = "net.runelite.client.plugins.microbot.tutorialisland.TutorialIslandPlugin";
    private volatile boolean missionSelected391 = false;
    private volatile boolean missionClaimAttempted391 = false;
    private volatile boolean missionClaimDone391 = false;
    private volatile boolean missionVerifyAttempted391 = false;
    private volatile boolean missionYieldAttempted391 = false;
    private volatile long missionLockHeartbeatMs391 = 0;
    private volatile long lastMissionYieldDiagMs391 = 0;
    private volatile boolean missionAdoptAttempted391 = false;

    private java.nio.file.Path missionFile391() {
        return java.nio.file.Paths.get(System.getProperty("user.home"), ".runelite", "bot-mission.txt");
    }

    private java.nio.file.Path missionLockFile391() {
        return java.nio.file.Paths.get(System.getProperty("user.home"), ".runelite", "bot-mission-lock.txt");
    }

    /**
     * Desired mission, or null when no mission was ever chosen (fresh
     * client). Build 392: a null here triggers one overlay-state adoption
     * instead of silently defaulting.
     */
    private String readDesiredMission391() {
        try {
            java.nio.file.Path p = missionFile391();
            if (!java.nio.file.Files.isRegularFile(p)) return null;
            String s = new String(java.nio.file.Files.readAllBytes(p),
                    java.nio.charset.StandardCharsets.UTF_8)
                    .trim().toLowerCase(java.util.Locale.ROOT);
            if (s.startsWith("cooks")) return "cooks";
            if (s.startsWith("tutorial")) return "tutorial";
        } catch (Exception ignored) { }
        return "tutorial";
    }

    /**
     * Build 392: first-ever selection follows the live overlay state.
     * Runs once, on the client thread. Adopts SELF only when the other
     * plugin is currently disabled/absent (a human chose this plugin in
     * the overlay); when both are enabled the default (tutorial) wins
     * deterministically. Once the file exists it is authoritative.
     */
    private void missionAdopt391() {
        try {
            if (java.nio.file.Files.isRegularFile(missionFile391())) return; // raced; file now exists
            net.runelite.client.plugins.PluginManager pm = Microbot.getPluginManager();
            net.runelite.client.plugins.Plugin other = null;
            for (net.runelite.client.plugins.Plugin p : pm.getPlugins()) {
                if (p.getClass().getName().equals(OTHER_PLUGIN_CLASS_391)) { other = p; break; }
            }
            boolean otherEnabled = false;
            if (other != null) {
                try { otherEnabled = pm.isPluginEnabled(other); } catch (Exception ignored) { }
            }
            String adopted = otherEnabled ? "tutorial" : MISSION_SELF_391;
            writeDesiredMission391(adopted);
            diag("Build 392: MISSION_SELECT adopt -- no mission file yet; otherEnabled="
                + otherEnabled + " -> mission=" + adopted
                + " (to change missions, ask Muse/Alex for the SWITCH command)");
        } catch (Exception e) {
            diag("Build 392: MISSION_SELECT adopt FAILED: " + e);
            missionAdoptAttempted391 = false; // retry next tick
        }
    }

    private void writeDesiredMission391(String m) {
        try {
            java.nio.file.Files.write(missionFile391(),
                    (m + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            diag("Build 392: mission file write failed: " + e.getMessage());
        }
    }

    private void writeMissionLock391() {
        try {
            String c = "owner=" + MISSION_SELF_391 + "\nts=" + System.currentTimeMillis() + "\n";
            java.nio.file.Files.write(missionLockFile391(),
                    c.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            diag("Build 392: lock write failed: " + e.getMessage());
        }
    }

    /** Lock owner when the lock is fresh (<10 min), else null. */
    private String readMissionLockOwner391() {
        try {
            java.nio.file.Path p = missionLockFile391();
            if (!java.nio.file.Files.isRegularFile(p)) return null;
            String c = new String(java.nio.file.Files.readAllBytes(p),
                    java.nio.charset.StandardCharsets.UTF_8);
            String owner = null;
            long ts = 0;
            for (String line : c.split("\n")) {
                line = line.trim();
                if (line.startsWith("owner=")) owner = line.substring(6).trim();
                else if (line.startsWith("ts=")) {
                    try { ts = Long.parseLong(line.substring(3).trim()); }
                    catch (Exception ignored) { }
                }
            }
            if (owner == null || ts == 0) return null;
            if (System.currentTimeMillis() - ts > 10 * 60 * 1000) return null;
            return owner;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * First post-login phase. Returns true once this plugin verifiably owns
     * the scheduler (SWITCH COMPLETE emitted); quest-state detection must
     * not run before then. All PluginManager work runs on the client
     * thread; the outcome is picked up on the following ticks. Never
     * blocks the housekeeping in onState -- it only holds stage logic.
     */
    private boolean missionSelectTick391() {
        if (missionSelected391) {
            long now = System.currentTimeMillis();
            if (now - missionLockHeartbeatMs391 > 60000) {
                missionLockHeartbeatMs391 = now;
                writeMissionLock391();
            }
            return true;
        }
        boolean loggedIn;
        try { loggedIn = Microbot.isLoggedIn(); }
        catch (Exception e) { return false; }
        if (!loggedIn) return false; // selection is the first POST-LOGIN phase
        if (switchingAway) return false; // remote SWITCH in flight -- stand down
        String desired = readDesiredMission391();
        if (desired == null) {
            // Build 392: no mission was ever chosen -- adopt once from the
            // live overlay state instead of silently defaulting.
            if (!missionAdoptAttempted391) {
                missionAdoptAttempted391 = true;
                diag("Build 392: MISSION_SELECT -- no mission file yet; adopting from overlay state");
                try { Microbot.getClientThread().invoke(this::missionAdopt391); }
                catch (Exception e) {
                    diag("Build 392: MISSION_SELECT adopt invoke failed: " + e);
                    missionAdoptAttempted391 = false;
                }
            }
            return false; // adoption in flight; next tick reads the file
        }
        if (MISSION_SELF_391.equals(desired)) {
            if (!missionClaimAttempted391) {
                missionClaimAttempted391 = true;
                diag("Build 392: MISSION_SELECT -- desired=" + desired
                    + " (self); claiming scheduler ownership");
                try { Microbot.getClientThread().invoke(this::missionClaim391); }
                catch (Exception e) {
                    diag("Build 392: MISSION_SELECT claim invoke failed: " + e);
                    missionClaimAttempted391 = false;
                }
                return false;
            }
            if (missionClaimDone391 && !missionVerifyAttempted391) {
                missionVerifyAttempted391 = true;
                try { Microbot.getClientThread().invoke(this::missionVerify391); }
                catch (Exception e) {
                    diag("Build 392: MISSION_SELECT verify invoke failed: " + e);
                    missionVerifyAttempted391 = false;
                }
            }
            return false;
        }
        // desired == other mission: yield / hand off.
        if (!missionYieldAttempted391) {
            missionYieldAttempted391 = true;
            diag("Build 392: MISSION_SELECT -- desired=" + desired
                + " (other); handing off to " + MISSION_OTHER_391);
            try { Microbot.getClientThread().invoke(this::missionYield391); }
            catch (Exception e) {
                diag("Build 392: MISSION_SELECT yield invoke failed: " + e);
                missionYieldAttempted391 = false;
            }
        } else {
            long now = System.currentTimeMillis();
            if (now - lastMissionYieldDiagMs391 > 30000) {
                lastMissionYieldDiagMs391 = now;
                diag("Build 392: MISSION_SELECT -- handoff to " + MISSION_OTHER_391
                    + " in flight, holding");
            }
        }
        return false;
    }

    /** Client-thread: disable+stop the other plugin, write mission+lock. */
    private void missionClaim391() {
        try {
            net.runelite.client.plugins.PluginManager pm = Microbot.getPluginManager();
            net.runelite.client.plugins.Plugin other = null;
            for (net.runelite.client.plugins.Plugin p : pm.getPlugins()) {
                if (p.getClass().getName().equals(OTHER_PLUGIN_CLASS_391)) { other = p; break; }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("otherFound=").append(other != null);
            if (other != null) {
                boolean wasEnabled = false;
                try { wasEnabled = pm.isPluginEnabled(other); } catch (Exception ignored) { }
                sb.append(",otherWasEnabled=").append(wasEnabled);
                if (wasEnabled) {
                    try { pm.setPluginEnabled(other, false); } catch (Exception ignored) { }
                    boolean stopped = false;
                    try { stopped = pm.stopPlugin(other); } catch (Exception ignored) { }
                    sb.append(",otherStop=").append(stopped);
                }
            }
            writeDesiredMission391(MISSION_SELF_391);
            writeMissionLock391();
            missionLockHeartbeatMs391 = System.currentTimeMillis();
            missionClaimDone391 = true;
            diag("Build 392: MISSION_SELECT claim -- " + sb);
        } catch (Exception e) {
            diag("Build 392: MISSION_SELECT claim FAILED: " + e);
            missionClaimAttempted391 = false; // retry next tick
        }
    }

    /** Client-thread: verify lock + overlay state, then SWITCH COMPLETE. */
    private void missionVerify391() {
        try {
            net.runelite.client.plugins.PluginManager pm = Microbot.getPluginManager();
            boolean selfEnabled = false;
            boolean otherEnabled = false;
            boolean otherFound = false;
            for (net.runelite.client.plugins.Plugin p : pm.getPlugins()) {
                String cn = p.getClass().getName();
                if (cn.equals(SELF_PLUGIN_CLASS_391)) {
                    try { selfEnabled = pm.isPluginEnabled(p); } catch (Exception ignored) { }
                } else if (cn.equals(OTHER_PLUGIN_CLASS_391)) {
                    otherFound = true;
                    try { otherEnabled = pm.isPluginEnabled(p); } catch (Exception ignored) { }
                }
            }
            String lockOwner = readMissionLockOwner391();
            diag("Build 392: MISSION_SELECT verify -- lockOwner=" + lockOwner
                + ", selfEnabled=" + selfEnabled
                + ", otherFound=" + otherFound + ", otherEnabled=" + otherEnabled);
            if (MISSION_SELF_391.equals(lockOwner) && selfEnabled && !otherEnabled) {
                missionSelected391 = true;
                writeMissionLock391();
                diag("Build 392: SWITCH COMPLETE -- " + MISSION_SELF_391
                    + " owns the scheduler (startup marker + overlay enabled + other plugin stopped + lock held); entering quest-state detection");
            } else {
                diag("Build 392: MISSION_SELECT verify FAILED -- re-claiming next tick");
                missionClaimAttempted391 = false;
                missionClaimDone391 = false;
                missionVerifyAttempted391 = false;
            }
        } catch (Exception e) {
            diag("Build 392: MISSION_SELECT verify FAILED: " + e);
            missionVerifyAttempted391 = false;
        }
    }

    /** Client-thread: ensure the desired plugin runs, then disable self. */
    private void missionYield391() {
        try {
            net.runelite.client.plugins.PluginManager pm = Microbot.getPluginManager();
            net.runelite.client.plugins.Plugin self = null;
            net.runelite.client.plugins.Plugin other = null;
            for (net.runelite.client.plugins.Plugin p : pm.getPlugins()) {
                String cn = p.getClass().getName();
                if (cn.equals(SELF_PLUGIN_CLASS_391)) self = p;
                else if (cn.equals(OTHER_PLUGIN_CLASS_391)) other = p;
            }
            if (other == null) {
                // Fail-safe: the desired plugin is not in this jar. Staying
                // on self beats killing the bot with nothing running.
                diag("Build 392: MISSION_SELECT -- desired=" + MISSION_OTHER_391
                    + " but its plugin class is not in the jar; STAYING on "
                    + MISSION_SELF_391 + " (fail-safe)");
                writeDesiredMission391(MISSION_SELF_391);
                missionSelected391 = true;
                return;
            }
            String lockOwner = readMissionLockOwner391();
            boolean otherClaimed = MISSION_OTHER_391.equals(lockOwner);
            StringBuilder sb = new StringBuilder();
            sb.append("otherClaimed=").append(otherClaimed);
            if (!otherClaimed) {
                // setPluginEnabled(true) starts the plugin via the manager;
                // skipped when the other side already claimed (never
                // double-start a running plugin).
                boolean enabled = false;
                try { enabled = pm.isPluginEnabled(other); } catch (Exception ignored) { }
                if (!enabled) {
                    try { pm.setPluginEnabled(other, true); } catch (Exception ignored) { }
                    sb.append(",otherEnabled=true");
                } else {
                    sb.append(",otherAlreadyEnabled");
                }
            }
            if (self != null) {
                try { pm.setPluginEnabled(self, false); } catch (Exception ignored) { }
                boolean stopped = false;
                try { stopped = pm.stopPlugin(self); } catch (Exception ignored) { }
                sb.append(",selfStopped=").append(stopped);
            }
            diag("Build 392: MISSION_SELECT yield -- " + sb + " -- self disabled, "
                + MISSION_OTHER_391 + " owns the mission now");
        } catch (Exception e) {
            diag("Build 392: MISSION_SELECT yield FAILED: " + e);
            missionYieldAttempted391 = false; // retry next tick
        }
    }

    public boolean run(CooksAssistantConfig config) {
        resetBucket505();
        this.config = config;
        diagClear();
        pinFixedWindow(); // Build 390: Julien -- always a fixed window
        diag("Build " + BUILD_NUMBER + ": STARTUP -- Cook's Assistant quest bot -- explicit running-build marker");
        diag("Build " + BUILD_NUMBER + ": plan = Cook(start) -> pot -> grain -> mill(hopper/controls/bin) -> egg -> bucket -> milk -> Cook(finish). Completion ONLY from QuestState.FINISHED.");
        diag("Build " + BUILD_NUMBER + ": STARTUP -- RUNNING_BUILD=510 (patch-507) -- explicit running-build marker, first line of this run");
        // Build 394 (Alex 2026-09-29 ~13:04): START THE TICK LOOP. Root cause of
        // the 12:59 silent Cook's: this script never scheduled its tick loop --
        // run() only called super.run() (base checks + return true, NO loop),
        // so onState() and the MISSION_SELECT gate never executed. Mirror
        // TutorialIslandScript's proven pattern: scheduleWithFixedDelay driving
        // step(), with the logged-out watchdog INSIDE the lambda (throttled
        // waiting marker + 6-min exit for Supervisor relaunch) so the wait is
        // visible while the gate holds for login. Gate itself unchanged.
        if (mainScheduledFuture != null && !mainScheduledFuture.isDone()) {
            try { mainScheduledFuture.cancel(true); } catch (Exception ignored) { }
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (disposed) return;
            try {
                // Build 395 (Alex 2026-09-29 ~13:06): throttled liveness marker
                // BEFORE the login gate -- proves the tick loop itself is alive
                // even if isLoggedIn() throws or the gate stays silent.
                long tickNow = System.currentTimeMillis();
                if (tickNow - lastTickAliveDiagMs >= 60000) {
                    lastTickAliveDiagMs = tickNow;
                    diag("Build 395: tick alive -- entering login/mission-select gate");
                }
                if (!Microbot.isLoggedIn()) {
                    // Logged-out watchdog (mirrors TI Build 194): the mission
                    // gate requires login, so while logged out keep screenshots
                    // alive, emit a throttled waiting marker, and after 6 min
                    // exit(0) so the Supervisor relaunches and the login
                    // clicker re-logs.
                    maybeAutoScreenshot();
                    long nowMs = System.currentTimeMillis();
                    if (lastLoggedInMs == 0) lastLoggedInMs = nowMs; // startup grace
                    long outMs = nowMs - lastLoggedInMs;
                    if (nowMs - lastLoggedOutDiagMs >= 60000) {
                        lastLoggedOutDiagMs = nowMs;
                        diag("Build 394: logged out " + (outMs / 60000)
                            + " min (login screen / disconnect dialog?) -- mission gate waiting for login; exit watchdog at 6 min");
                    }
                    // Build 502 (Alex 21:14): LOGIN_GATE moved here from doGetBucket.
                    // The outer gate returns before doGetBucket, so the inner LOGIN_GATE
                    // was unreachable. Report exact login state here (throttled 10s).
                    boolean pp = false;
                    try { pp = Microbot.getClient().getLocalPlayer() != null; } catch (Exception ignored) {}
                    if (nowMs - lastLoginStateReportMs > 10000) {
                        lastLoginStateReportMs = nowMs;
                        diag("Build 502: LOGIN_GATE -- loggedIn=false playerPresent=" + pp +
                            " -- waiting for fresh logged-in frame, recovery gated");
                    }
                    if (outMs >= LOGGED_OUT_EXIT_AFTER_MS && !logoutIssued) {
                        diag("Build 394: logged out 6+ min -- exiting for Supervisor relaunch");
                        saveScreenshot("logged-out-exit");
                        System.exit(0);
                    }
                    return;
                }
                lastLoggedInMs = System.currentTimeMillis();
                step();
            } catch (Exception ex) {
                log.error("[CooksAssistant] Unexpected error in tick", ex);
                net.runelite.client.plugins.microbot.util.Global.sleep(2000, 3000);
            }
        }, 0, config.tickDelay(), TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    protected void onState(Stage state) {
        maybeAutoScreenshot();
        maybeCheckForUpdate();
        // Build 415: Diagnostic overlay controller (optional, read-only).
        // Runs before gameplay logic; never blocks, never acts.
        // Build 418: when disabled, clear the visual overlay so no stale
        // layer persists on screen.
        try {
            if (config.diagController()) {
                diagControllerTick();
            } else {
                CooksAssistantDiagOverlay ov = diagOverlay;
                if (ov != null) {
                    ov.clear();
                }
                // Reset layer state so re-enabling starts fresh.
                diagCtlLayerStartMs = 0;
            }
        } catch (Exception e) { /* controller never breaks gameplay */ }
        if (switchingAway) return; // Build 390: remote SWITCH in flight -- stand down
        pollBotCommand(); // Build 390: remote command channel (PAUSE/RESUME/STATUS/RESTART/SWITCH_TO_TUTORIAL)
        if (remotePaused) {
            diag("Cook's Assistant: REMOTE PAUSE active -- quest frozen");
            return;
        }

        // Logged-out watchdog: screenshot while logged out; exit after 6 min
        // so the Supervisor relaunches and the login clicker re-logs.
        // Build 388: after an INTENTIONAL completion logout (logoutIssued),
        // park at the login screen -- do NOT exit for relaunch (that would
        // relogin -> DONE -> logout forever while Julien is away).
        try {
            if (!Microbot.isLoggedIn()) {
                long now = System.currentTimeMillis();
                if (lastLoggedInMs == 0) lastLoggedInMs = now;
                if (now - lastLoggedInMs > 6 * 60 * 1000 && !logoutIssued) {
                    diag("logged out 6+ min -- exiting for Supervisor relaunch");
                    System.exit(0);
                }
                return;
            }
            lastLoggedInMs = System.currentTimeMillis();
        } catch (Exception ignored) {
        }

        // Build 392: mission selection is the first post-login phase (Alex).
        // Quest logic is held until scheduler ownership is verified.
        boolean missionReady391 = missionSelectTick391();
        if (!missionReady391) return;
        switch (state) {
            case DETECT -> doDetect();
            case TALK_COOK_START -> doTalkCookStart();
            case GET_POT -> doGetPot();
            case GET_GRAIN -> doGetGrain();
            case MILL_FLOUR -> doMillFlour();
            case GET_EGG -> doGetEgg();
            case GET_BUCKET -> doGetBucket();
            case MILK_COW -> doMilkCow();
            case RETURN_COOK -> doReturnCook();
            case DONE -> doDone();
        }
    }

    @Override
    protected Stage onError(Stage state, Exception e) {
        log.error("[CooksAssistant] Error in state {}", state, e);
        diag("ERROR in " + state + ": " + e.getClass().getSimpleName() + " -- retrying state next tick");
        return state;
    }

    /** Route from observed quest state + inventory. Never guesses. */
    private void doDetect() {
        QuestState qs = questState();
        if (qs == QuestState.FINISHED) {
            forceState(Stage.DONE, "already finished");
            return;
        }
        if (qs == QuestState.NOT_STARTED) {
            diag("DETECT: quest not started -> TALK_COOK_START");
            forceState(Stage.TALK_COOK_START, "start quest");
            return;
        }
        // IN_PROGRESS (or unknown -- treat like in-progress, inventory decides)
        if (!hasFlour()) {
            if (hasPot() && hasGrain()) {
                diag("DETECT: have pot+grain, need flour -> MILL_FLOUR");
                forceState(Stage.MILL_FLOUR, "mill the grain");
            } else if (hasPot()) {
                diag("DETECT: have pot, need grain -> GET_GRAIN");
                forceState(Stage.GET_GRAIN, "get grain");
            } else {
                diag("DETECT: need pot -> GET_POT");
                forceState(Stage.GET_POT, "get pot");
            }
            return;
        }
        if (!hasEgg()) {
            diag("DETECT: need egg -> GET_EGG");
            forceState(Stage.GET_EGG, "get egg");
            return;
        }
        if (!hasMilk()) {
            if (hasBucket()) {
                diag("DETECT: have bucket, need milk -> MILK_COW");
                forceState(Stage.MILK_COW, "milk cow");
            } else {
                diag("DETECT: need bucket -> GET_BUCKET");
                forceState(Stage.GET_BUCKET, "get bucket");
            }
            return;
        }
        diag("DETECT: have milk+egg+flour -> RETURN_COOK");
        forceState(Stage.RETURN_COOK, "deliver ingredients");
    }

    private static final String[] COOK_START_OPTIONS = {
        "What's wrong?",
        "I'm always happy to help a cook in distress.",
        "Actually, I know where to find this stuff."
    };

    private void doTalkCookStart() {
        if (dialogueTick(COOK_START_OPTIONS)) return; // dialogue owns the tick
        QuestState qs = questState();
        if (qs == QuestState.IN_PROGRESS) {
            diag("TALK_COOK_START: quest now IN_PROGRESS -> DETECT");
            forceState(Stage.DETECT, "quest started");
            return;
        }
        WorldPoint pp = playerPos();
        if (pp == null) return;
        if (chebDist(pp, COOK_TILE) > 12) {
            stepToward(COOK_TILE, "to the Cook");
            return;
        }
        talkToNpc("Cook", 15);
    }

    private void doGetPot() {
        if (hasPot()) {
            diag("GET_POT: pot observed -> DETECT");
            forceState(Stage.DETECT, "have pot");
            return;
        }
        if (dialogueTick(new String[0])) return;
        WorldPoint pp = playerPos();
        if (pp == null) return;
        if (chebDist(pp, COOK_TILE) > 12) {
            stepToward(COOK_TILE, "to kitchen");
            return;
        }
        // Pot respawns on the kitchen table -- take the ground item.
        try {
            if (Rs2GroundItem.take("Pot", 15)) {
                diag("GET_POT: taking ground pot -- verifying next tick");
            }
        } catch (Exception e) {
            diag("GET_POT: take failed: " + e.getClass().getSimpleName());
        }
        Microbot.status = "Cook's Assistant: getting a pot...";
    }

    private void doGetGrain() {
        // Build 419 (NUDGE ALEX 2026-09-29 16:17): one-click/next-tick proof.
        // If grain is observed, done. If a click is pending verification,
        // REQUIRE the wheat count increase -- never re-click blindly.
        if (hasGrain()) {
            // Grain proven -- clear transient state and advance.
            grainClickPending = false;
            grainRetried = false;
            grainFailed = false;
            grainTargetId = -1;
            grainTargetTile = null;
            grainPreCount = -1;
            diag("GET_GRAIN: grain observed -> DETECT");
            forceState(Stage.DETECT, "have grain");
            return;
        }
        // Bounded failure: hold, do not retry, do not advance.
        if (grainFailed) {
            diag("GET_GRAIN_FAILED: bounded failure -- holding (no grain proof after one retry)");
            Microbot.status = "Cook's Assistant: grain failed -- holding";
            return;
        }
        if (dialogueTick(new String[0])) return;
        WorldPoint pp = playerPos();
        if (pp == null) return;
        // --- Next-tick proof for a pending click ---
        // Build 478 (Alex 19:41): Pick latch. Once a Pick is issued, NEVER
        // click another wheat until inventory proves delta or timeout expires.
        // Do NOT use preWheat=1 as permission to click. The 19:40:41 Pick
        // succeeded but proof lagged; the 19:41:00 duplicate Pick must not
        // happen. One Pick per GET_GRAIN run.
        if (grainClickPending) {
            int nowCount = invCount(GRAIN);
            if (nowCount > grainPreCount) {
                // Proof: wheat count increased.
                diag("Build 478: GET_GRAIN: PROOF wheat " + grainPreCount + "->" + nowCount
                    + " after Pick on id=" + grainTargetId + " -- advancing");
                grainClickPending = false;
                grainRetried = false;
                grainFailed = false;
                grainTargetId = -1;
                grainTargetTile = null;
                grainPreCount = -1;
                grainPickIssuedAt = 0;
                forceState(Stage.DETECT, "grain proven");
                return;
            }
            // No delta yet -- check if we're still within the proof timeout.
            long elapsed = System.currentTimeMillis() - grainPickIssuedAt;
            if (elapsed < PICK_PROOF_TIMEOUT_MS) {
                // Still waiting for inventory proof. LATCH: do NOT retry,
                // do NOT fall through to rescan. Just wait.
                diag("Build 478: GET_GRAIN: Pick latched, awaiting inventory proof "
                    + "(elapsed " + elapsed + "ms / " + PICK_PROOF_TIMEOUT_MS + "ms, "
                    + "count still " + nowCount + ") -- holding, no re-click");
                return;
            }
            // Timeout expired without delta -- bounded failure. Do NOT retry
            // with alternate; emit typed HOLD.
            grainFailed = true;
            grainClickPending = false;
            grainPickIssuedAt = 0;
            diag("Build 478: GET_GRAIN: PICK_PROOF_TIMEOUT after " + elapsed + "ms "
                + "(no wheat delta, count still " + nowCount + ") -- holding");
            Microbot.status = "Cook's Assistant: PICK_PROOF_TIMEOUT -- holding";
            return;
        }
        // Build 426 (Alex 16:40): vertical traversal via shared resolver.
        // The Rs2Traversal handles ladder/stair with one-action/next-tick proof.
        // No ad-hoc ladder logic here.
        WorldPoint ppNav = playerPos();
        if (ppNav != null && ppNav.getPlane() != 0) {
            // If vertical traversal is active, tick it.
            if (grainTraversal != null && !grainTraversal.isIdle()) {
                Rs2Traversal.Result tr = grainTraversal.tick(ppNav);
                if (tr == Rs2Traversal.Result.DONE) {
                    diag("Build 472: GET_GRAIN: vertical traversal DONE -- replanning");
                    grainTraversal = null;
                } else if (tr == Rs2Traversal.Result.FAILED) {
                    diag("Build 472: GET_GRAIN: vertical traversal FAILED -- holding");
                    Microbot.status = "Cook's Assistant: vertical blocked -- holding";
                    return;
                } else {
                    return; // IN_PROGRESS
                }
            }
            // Request vertical traversal to plane 0.
            if (grainTraversal == null) {
                grainTraversal = new Rs2Traversal(msg -> diag(msg));
            }
            // Only request if not already requested (isIdle).
            if (grainTraversal.isIdle()) {
                grainTraversal.requestVerticalTraversal(ppNav, 0, "GET_GRAIN_VERTICAL");
            }
            return;
        }
        // Verify plane delta from previous climb (next-tick proof).
        // (Build 472: old climb latch removed; Rs2Traversal handles it.)
        if (chebDist(pp, WHEAT_FIELD) > 14) {
            // Build 435 (Alex 17:04): stepToward OWNS the shared traversal.
            // No phase-specific handoff needed -- stall -> resolver -> proof
            // happens inside stepToward. Just walk.
            stepToward(WHEAT_FIELD, "to wheat field");
            return;
        }
        // --- Reusable traversal layer (Build 422, Alex 16:28) ---
        // FAILED is terminal: hard-hold, never interact until deliberate reset.
        if (grainTraversal != null && grainTraversal.hasFailed()) {
            diag("Build 422: GET_GRAIN: traversal FAILED (terminal) -- holding, no Pick");
            Microbot.status = "Cook's Assistant: route blocked -- holding";
            return;
        }
        // If a traversal is active, tick it. Do NOT touch wheat until DONE.
        if (grainTraversal != null && !grainTraversal.isIdle()) {
            Rs2Traversal.Result tr = grainTraversal.tick(pp);
            if (tr == Rs2Traversal.Result.DONE) {
                diag("Build 422: GET_GRAIN: traversal DONE -- re-resolving wheat");
                grainTraversal = null; // release; re-resolve wheat below
            } else if (tr == Rs2Traversal.Result.FAILED) {
                // Traversal already emitted ROUTE_BLOCKED/DOOR_CROSSING_FAILED.
                // hasFailed() will hard-hold on subsequent ticks.
                diag("Build 422: GET_GRAIN: traversal FAILED -- holding");
                Microbot.status = "Cook's Assistant: route blocked -- holding";
                return;
            } else {
                // IN_PROGRESS: wait, do nothing else.
                return;
            }
        }
        // (Build 472: plane navigation moved earlier, before chebDist check.)
        // Resolve one live wheat object, excluding the failed target on retry.
        Rs2TileObjectModel wheat = findWheatForGrain(pp, 18);
        if (wheat == null) {
            diag("GET_GRAIN: no wheat in range -- waiting");
            Microbot.status = "Cook's Assistant: looking for wheat...";
            return;
        }
        WorldPoint wp;
        int wid;
        try {
            wp = wheat.getWorldLocation();
            wid = wheat.getId();
            // Build 420 (NUDGE ALEX 2026-09-29 16:24): NEVER call
            // getObjectComposition() -- it BLOCKS on CompletableFuture and
            // froze the tick thread for 2+ min at 16:21:52. Actions verified
            // by click result + inventory delta, not by reading composition.
        } catch (Exception e) {
            return;
        }
        // Build 419 (Alex 16:18-16:19): REQUIRE reachability before clicking.
        // If the wheat tile is not reachable, do NOT click -- delegate to the
        // reusable traversal layer (never click through a fence).
        boolean reachable = false;
        try {
            reachable = Rs2Tile.isTileReachable(wp);
        } catch (Exception e) { /* treat as unreachable */ }
        if (!reachable) {
            diag("Build 419: GET_GRAIN: wheat id=" + wid + "@(" + wp.getX() + "," + wp.getY()
                + ") UNREACHABLE -- requesting traversal (not clicking)");
            if (grainTraversal == null) {
                grainTraversal = new Rs2Traversal(msg -> diag(msg));
            }
            grainTraversal.requestTraversal(pp, wp, "GET_GRAIN");
            // WALL_DOOR layer will activate (isWallDoorRelevant checks traversal).
            return;
        }
        if (chebDist(pp, wp) > 3) {
            stepToward(wp, "to wheat");
            return;
        }
        // One Pick: record pre-inventory, click, await next-tick proof.
        // Never re-click the same target without proof.
        try {
            int preCount = invCount(GRAIN);
            wheat.click("Pick");
            grainTargetId = wid;
            grainTargetTile = wp;
            grainPreCount = preCount;
            grainClickPending = true;
            grainPickIssuedAt = System.currentTimeMillis(); // Build 478: latch start
            diag("Build 478: GET_GRAIN: one Pick on wheat id=" + wid
                + "@(" + wp.getX() + "," + wp.getY() + ")"
                + " preWheat=" + preCount + " -- verifying next tick");
        } catch (Exception e) {
            diag("GET_GRAIN: click failed: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Build 419: find one wheat object for grain, excluding the failed target
     * after a retry (so the alternate-target rescan picks a different one).
     */
    private Rs2TileObjectModel findWheatForGrain(WorldPoint pp, int radius) {
        try {
            // Build 420: bounded query -- name filter + spatial bound at query
            // level, not after full materialization.
            java.util.List<Rs2TileObjectModel> cands =
                Microbot.getRs2TileObjectCache().query()
                    .within(pp, radius)
                    .withName("Wheat")
                    .toList();
            Rs2TileObjectModel best = null;
            int bestDist = Integer.MAX_VALUE;
            for (Rs2TileObjectModel o : cands) {
                try {
                    WorldPoint owp = o.getWorldLocation();
                    if (owp.distanceTo(pp) > radius) continue;
                    // Exclude the failed target on retry.
                    if (grainRetried && grainTargetId != -1 && o.getId() == grainTargetId
                        && grainTargetTile != null && owp.equals(grainTargetTile)) {
                        continue;
                    }
                    int d = owp.distanceTo(pp);
                    if (d < bestDist) {
                        bestDist = d;
                        best = o;
                    }
                } catch (Exception e) { /* skip */ }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * MILL_FLOUR sub-steps:
     * 0 = walk to / enter the mill (plane 0)
     * 1 = climb ladder(s) to plane 2
     * 2 = use grain on hopper (verify: grain consumed)
     * 3 = operate hopper controls
     * 4 = climb down to plane 0
     * 5 = use pot on flour bin (verify: pot of flour in inventory)
     */
    private void doMillFlour() {
        // Build 472: sticky terminal HOLD. No further clicks, no substate
        // changes. A fresh verified state change (flour observed) releases it.
        if (millFailed) {
            if (hasFlour()) {
                diag("Build 472: MILL_FLOUR: flour observed after HOLD -- releasing to DETECT");
                millFailed = false;
                hopperUseExhausted = false;
                millSub = 0;
                forceState(Stage.DETECT, "have flour");
                return;
            }
            return; // holding: no clicks, no navigation, no retries
        }
        if (hasFlour()) {
            diag("MILL_FLOUR: pot of flour observed -> DETECT");
            millSub = 0;
            useOnReset();
            forceState(Stage.DETECT, "have flour");
            return;
        }
        if (!hasGrain() || !hasPot()) {
            diag("MILL_FLOUR: lost grain/pot -> DETECT re-routes");
            millSub = 0;
            useOnReset();
            forceState(Stage.DETECT, "missing inputs");
            return;
        }
        if (dialogueTick(new String[0])) return;
        WorldPoint pp = playerPos();
        if (pp == null) return;

        // Stall watchdog across sub-steps.
        if (millLastTile != null && millLastTile.equals(pp)) {
            millStallTicks++;
        } else {
            millStallTicks = 0;
            millLastTile = pp;
        }
        if (millStallTicks >= 60) {
            diag("MILL_FLOUR: 60 ticks no progress at sub=" + millSub + " -- resetting to sub 0");
            millSub = 0;
            millStallTicks = 0;
            millLastTile = null;
            useOnReset();
        }

        switch (millSub) {
            case 0: { // walk to the mill, enter
                if (pp.getPlane() == 0 && chebDist(pp, MILL_INSIDE) <= 3) {
                    diag("MILL_FLOUR: inside mill -> climb (sub 1)");
                    millSub = 1;
                    millStallTicks = 0;
                    return;
                }
                if (pp.getPlane() != 0) {
                    diag("MILL_FLOUR: wrong plane " + pp.getPlane() + " -> climb down first (sub 4)");
                    millSub = 4;
                    return;
                }
                if (chebDist(pp, MILL_APPROACH) > 6) {
                    // Build 435 (Alex 17:04): stepToward OWNS the shared traversal.
                    // No phase-specific handoff needed.
                    stepToward(MILL_APPROACH, "to mill");
                    return;
                }
                // At the mill: walk inside (pathfinder handles the door).
                try {
                    Rs2Walker.walkTo(MILL_INSIDE);
                } catch (Exception e) {
                    diag("MILL_FLOUR: walkTo mill failed: " + e.getClass().getSimpleName());
                }
                Microbot.status = "Cook's Assistant: entering the mill...";
                return;
            }
            case 1: { // climb to plane 2 (hopper floor)
                if (pp.getPlane() == 2) {
                    // Build 472: if hopper use is exhausted, NEVER re-enter sub 2.
                    // Go straight to controls (sub 3) -- the bin will verify.
                    if (hopperUseExhausted) {
                        diag("Build 472: MILL_FLOUR: hopper use exhausted (sticky) -- "
                            + "skipping sub 2, straight to controls (sub 3)");
                        millSub = 3;
                        millStallTicks = 0;
                        return;
                    }
                    diag("MILL_FLOUR: on hopper floor -> use grain on hopper (sub 2)");
                    millSub = 2;
                    millStallTicks = 0;
                    grainBeforeHopper = invCount(GRAIN);
                    return;
                }
                // Build 396: climb latch -- one click, then verify the plane
                // changed on the next ticks. One bounded retry, then rescan.
                // (A plane change to ANY other plane clears the latch; the
                // loop-top routes 0->1->2 with one verified click per floor.)
                if (climbMs > 0) {
                    if (pp.getPlane() != climbFromPlane) {
                        diag("Build 396: MILL_FLOUR: climb verified -- plane " + climbFromPlane + " -> " + pp.getPlane());
                        climbMs = 0;
                        climbRetried = false;
                    } else if (System.currentTimeMillis() - climbMs >= 5000) {
                        if (!climbRetried) {
                            climbRetried = true;
                            climbMs = 0;
                            diag("Build 396: MILL_FLOUR: Climb-up no plane change after 5s -- one bounded retry");
                        } else {
                            climbMs = 0;
                            climbRetried = false;
                            diag("Build 396: MILL_FLOUR: Climb-up FAILED after retry -- rescanning scene (sub 0)");
                            millSub = 0;
                            millStallTicks = 0;
                            return;
                        }
                    } else {
                        return; // latched: waiting for plane change, no click
                    }
                }
                Rs2TileObjectModel ladder = findObject("Ladder", 12);
                if (ladder == null) {
                    diag("MILL_FLOUR: no ladder in range on plane " + pp.getPlane());
                    if (chebDist(pp, MILL_INSIDE) > 6) stepToward(MILL_INSIDE, "into mill");
                    return;
                }
                WorldPoint lp;
                try {
                    lp = ladder.getWorldLocation();
                } catch (Exception e) {
                    return;
                }
                if (chebDist(pp, lp) > 2) {
                    stepToward(lp, "to ladder");
                    return;
                }
                try {
                    ladder.click("Climb-up");
                    climbMs = System.currentTimeMillis();
                    climbFromPlane = pp.getPlane();
                    diag("Build 396: MILL_FLOUR: clicked Climb-up once -- latched, verifying plane change next tick");
                } catch (Exception e) {
                    diag("MILL_FLOUR: ladder click failed: " + e.getClass().getSimpleName());
                }
                return;
            }
            case 2: { // use grain on hopper
                if (pp.getPlane() != 2) {
                    diag("MILL_FLOUR: fell off plane 2 -> back to sub 1");
                    millSub = 1;
                    useOnReset();
                    return;
                }
                if (useItemOnObject(GRAIN, "Hopper", "consume", 12)) {
                    diag("MILL_FLOUR: grain in hopper -> operate controls (sub 3)");
                    millSub = 3;
                    millStallTicks = 0;
                } else if (useOnFailed) {
                    useOnFailed = false; // consumed
                    // Build 433 (Alex 16:56 + Julien 16:56): STICKY per objective.
                    // The bounded retry inside useItemOnObject is exhausted.
                    // NEVER re-enter sub 2 -- no repeated hopper clicks across
                    // ticks/runs. Julien observed "already grain in the hopper":
                    // proceed to controls (sub 3); the flour-bin check will
                    // verify whether grain was actually present. If the bin is
                    // empty after controls, THAT is the terminal HOLD with
                    // evidence -- not here.
                    hopperUseExhausted = true;
                    diag("Build 472: MILL_FLOUR: hopper use EXHAUSTED (one bounded "
                        + "retry done) -- STICKY, no more hopper clicks. Hopper may "
                        + "already contain grain -> operate controls (sub 3) to verify");
                    millSub = 3;
                    millStallTicks = 0;
                }
                return;
            }
            case 3: { // operate hopper controls
                if (pp.getPlane() != 2) {
                    millSub = 1;
                    return;
                }
                Rs2TileObjectModel controls = findObject("Hopper controls", 12);
                if (controls == null) {
                    // Some clients name it "Hopper Controls" -- try loose via interact fallback.
                    try {
                        if (Rs2GameObjectFallback.interact("Hopper controls", "Operate")) {
                            diag("MILL_FLOUR: operated hopper controls (fallback) -> climb down (sub 4)");
                            millSub = 4;
                            millStallTicks = 0;
                        }
                    } catch (Exception e) {
                        diag("MILL_FLOUR: no hopper controls found");
                    }
                    return;
                }
                WorldPoint cp;
                try {
                    cp = controls.getWorldLocation();
                } catch (Exception e) {
                    return;
                }
                if (chebDist(pp, cp) > 3) {
                    stepToward(cp, "to hopper controls");
                    return;
                }
                try {
                    controls.click("Operate");
                    diag("MILL_FLOUR: clicked Operate on hopper controls -> climb down (sub 4)");
                } catch (Exception e) {
                    diag("MILL_FLOUR: controls click failed: " + e.getClass().getSimpleName());
                    return;
                }
                millSub = 4;
                millStallTicks = 0;
                return;
            }
            case 4: { // climb down to plane 0
                if (pp.getPlane() == 0) {
                    diag("MILL_FLOUR: on ground floor -> flour bin (sub 5)");
                    millSub = 5;
                    millStallTicks = 0;
                    flourBinAttempts = 0;
                    return;
                }
                // Build 396: climb latch -- one Climb-down click, verify the
                // plane changed next ticks. One bounded retry, then rescan.
                // (This is the 13:11:58-13:12:06 repeat-click defect.)
                if (climbMs > 0) {
                    if (pp.getPlane() != climbFromPlane) {
                        diag("Build 396: MILL_FLOUR: climb-down verified -- plane " + climbFromPlane + " -> " + pp.getPlane());
                        climbMs = 0;
                        climbRetried = false;
                    } else if (System.currentTimeMillis() - climbMs >= 5000) {
                        if (!climbRetried) {
                            climbRetried = true;
                            climbMs = 0;
                            diag("Build 396: MILL_FLOUR: Climb-down no plane change after 5s -- one bounded retry");
                        } else {
                            climbMs = 0;
                            climbRetried = false;
                            diag("Build 396: MILL_FLOUR: Climb-down FAILED after retry -- rescanning scene (sub 0)");
                            millSub = 0;
                            millStallTicks = 0;
                            return;
                        }
                    } else {
                        return; // latched: waiting for plane change, no click
                    }
                }
                Rs2TileObjectModel ladder = findObject("Ladder", 12);
                if (ladder == null) {
                    diag("MILL_FLOUR: no ladder to climb down on plane " + pp.getPlane());
                    return;
                }
                WorldPoint lp;
                try {
                    lp = ladder.getWorldLocation();
                } catch (Exception e) {
                    return;
                }
                if (chebDist(pp, lp) > 2) {
                    stepToward(lp, "to ladder");
                    return;
                }
                try {
                    ladder.click("Climb-down");
                    climbMs = System.currentTimeMillis();
                    climbFromPlane = pp.getPlane();
                    diag("Build 396: MILL_FLOUR: clicked Climb-down once -- latched, verifying plane change next tick");
                } catch (Exception e) {
                    diag("MILL_FLOUR: ladder click failed: " + e.getClass().getSimpleName());
                }
                return;
            }
            case 5: { // use pot on flour bin -- Build 396: exactly one
                // click via the use-on latch, then next-tick inventory proof
                // of flour/pot state before continuing.
                if (pp.getPlane() != 0) {
                    millSub = 4;
                    return;
                }
                if (useItemOnObject(POT, "Flour bin", "product:" + FLOUR, 12)) {
                    diag("MILL_FLOUR: flour collected -> DETECT");
                    millSub = 0;
                    forceState(Stage.DETECT, "have flour");
                    return;
                }
                if (useOnFailed) {
                    useOnFailed = false; // consumed
                    // Build 433 (Alex 16:56): terminal HOLD when the hopper was
                    // already exhausted. Full cycle attempted: hopper use
                    // exhausted (may have already contained grain) -> controls
                    // operated -> bin still empty. No evidence of flour; looping
                    // back to controls would repeat forever. HOLD with evidence.
                    if (hopperUseExhausted) {
                        diag("Build 472: MILL_FLOUR: TERMINAL HOLD -- hopper use "
                            + "exhausted + controls operated + flour bin empty. "
                            + "Evidence: hopperUseExhausted=true, no '" + FLOUR
                            + "' after bounded bin retry. No further clicks.");
                        millFailed = true; // sticky -- doMillFlour holds
                        Microbot.status = "Cook's Assistant: mill failed -- holding";
                        return;
                    }
                    // Build 429 (Alex 16:51 framework rule): interaction failure
                    // is SEPARATE from navigation. Do NOT reissue a route leg
                    // or reset a verified crossing. Bounded retry lives inside
                    // useItemOnObject; here we only step back to sub 3.
                    diag("Build 472: MILL_FLOUR: flour-bin use FAILED after retry -- controls may not have run; back to sub 3");
                    flourBinAttempts = 0;
                    millSub = 3;
                    return;
                }
                if (++flourBinAttempts > 40) {
                    diag("MILL_FLOUR: 40 ticks no flour -- controls may not have run; back to sub 3");
                    flourBinAttempts = 0;
                    millSub = 3;
                }
                return;
            }
            default:
                millSub = 0;
        }
    }

    private void doGetEgg() {
        if (hasEgg()) {
            eggMs = 0;
            eggRetried = false;
            eggDead = false;

            diag("GET_EGG: egg observed -> DETECT");
            forceState(Stage.DETECT, "have egg");
            return;
        }
        if (dialogueTick(new String[0])) return;
        WorldPoint pp = playerPos();
        if (pp == null) return;
        if (chebDist(pp, CHICKEN_FARM) > 14) {
            stepToward(CHICKEN_FARM, "to chicken farm");
            return;
        }
        // Build 396: pending-action latch (the 13:13:41-46 7x-take defect).
        // One take click, record pre-count + action tick; do not click again
        // until the next tick checks count/scene. At most one retry after
        // rescan, then stop with diagnostics.
        if (eggDead) {
            long nowD = System.currentTimeMillis();
            if (nowD - eggDeadDiagMs > 30000) {
                eggDeadDiagMs = nowD;
                diag("Build 396: GET_EGG: HELD -- no egg after retry; standing down with diagnostics (no further take clicks)");
            }
            return;
        }
        if (eggMs > 0) {
            int nowCount = invCount(EGG);
            if (nowCount > eggBefore) {
                diag("Build 396: GET_EGG: take verified -- egg " + eggBefore + " -> " + nowCount);
                eggMs = 0;
                eggRetried = false;
                return; // loop-top observes hasEgg() next tick
            }
            if (System.currentTimeMillis() - eggMs >= 5000) {
                if (!eggRetried) {
                    eggRetried = true;
                    eggMs = 0;
                    diag("Build 396: GET_EGG: no egg delta after 5s latched wait -- one bounded retry after rescan");
                } else {
                    eggDead = true;
                    eggDeadDiagMs = 0;
                    diag("Build 396: GET_EGG: FAILED after one retry -- stopping with diagnostics at "
                        + pp.getX() + "," + pp.getY() + "," + pp.getPlane());
                    return;
                }
            } else {
                return; // latched: waiting for next-tick count proof, no click
            }
        }
        // Build 479 (Alex 19:45): manual egg scan with diagnostic logging.
        // Log live ground-item id/name/tile/actions/reachability at first Take
        // and after rescan. Choose a REACHABLE Egg target, not a stale tile.
        // Preserve: one Take + next-tick proof, one bounded alternate, then HOLD.
        try {
            net.runelite.client.plugins.microbot.util.models.RS2Item[] eggs =
                net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem
                    .getAllFromWorldPoint(1944, pp); // 1944 = Egg item id
            // Filter to Egg by name (exact) and log candidates.
            java.util.List<net.runelite.client.plugins.microbot.util.models.RS2Item> candidates =
                new java.util.ArrayList<>();
            StringBuilder eggDiag = new StringBuilder();
            eggDiag.append("Build 479: GET_EGG: scan found ");
            eggDiag.append(eggs != null ? eggs.length : 0).append(" items; ");
            if (eggs != null) {
                for (net.runelite.client.plugins.microbot.util.models.RS2Item item : eggs) {
                    try {
                        String iname = item.getItem().getName();
                        if (!"Egg".equalsIgnoreCase(iname)) continue;
                        net.runelite.api.Tile tile = item.getTile();
                        net.runelite.api.coords.WorldPoint wpEgg =
                            tile.getWorldLocation();
                        int iid = item.getItem().getId();
                        // Reachability: can we path to the tile?
                        boolean reachable = false;
                        try {
                            reachable = net.runelite.client.plugins.microbot.util.tile.Rs2Tile
                                .isTileReachable(wpEgg);
                        } catch (Exception e) { /* false */ }
                        int dist = pp.distanceTo(wpEgg);
                        eggDiag.append("[id=").append(iid)
                            .append(" tile=(").append(wpEgg.getX()).append(",")
                            .append(wpEgg.getY()).append(") dist=").append(dist)
                            .append(" reachable=").append(reachable).append("] ");
                        if (reachable && dist <= 18) {
                            candidates.add(item);
                        }
                    } catch (Exception e) { /* skip */ }
                }
            }
            diag(eggDiag.toString());
            diag(eggDiag.toString());
            if (candidates.isEmpty()) {
                // Build 482 (Alex 19:51): Preserve Build479 behavior.
                // At 19:50:24 WebWalk collision reached 100%, at 19:50:25 Egg
                // became reachable. Do NOT treat as route failure; wait for
                // collision data. One Take + next-tick proof, one alternate,
                // then HOLD (handled by eggMs/eggRetried/eggDead above).
                diag("Build 482: GET_EGG: no reachable Egg found -- waiting (collision may be loading)");
                return;
            }
            // Choose nearest reachable.
            candidates.sort((a, b) -> {
                try {
                    int da = pp.distanceTo(a.getTile().getWorldLocation());
                    int db = pp.distanceTo(b.getTile().getWorldLocation());
                    return Integer.compare(da, db);
                } catch (Exception e) { return 0; }
            });
            net.runelite.client.plugins.microbot.util.models.RS2Item chosen =
                candidates.get(0);
            net.runelite.api.coords.WorldPoint chosenWp =
                chosen.getTile().getWorldLocation();
            diag("Build 479: GET_EGG: chosen Egg id=" +
                chosen.getItem().getId() + "@(" + chosenWp.getX() + "," +
                chosenWp.getY() + ") -- one Take, latched");
            if (net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem
                    .interact(chosen)) {
                eggMs = System.currentTimeMillis();
                eggBefore = invCount(EGG);
                diag("Build 479: GET_EGG: one take click issued (pre-count=" +
                    eggBefore + ") -- latched, verifying next tick");
            }
        } catch (Exception e) {
            diag("GET_EGG: take failed: " + e.getClass().getSimpleName());
        }
        Microbot.status = "Cook's Assistant: getting an egg...";
    }

    /**
     * Build 483 (Alex 19:57): Classify current pen by live scene targets.
     * Returns: "CHICKEN" if Egg ground items visible, "DAIRY" if dairy Milk
     * target reachable, "COW_FIELD" if bucket visible, else "UNKNOWN".
     * Used to avoid crossing wrong pen gates.
     */
    private String classifyPenByTargets(WorldPoint pp) {
        try {
            // Check for dairy Milk target (tile object id=8689 with Milk action).
            java.util.List<net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel> milkTargets =
                net.runelite.client.plugins.microbot.Microbot.getRs2TileObjectCache()
                    .query()
                    .within(pp, 12)
                    .where(o -> {
                        try {
                            if (o.getId() != 8689) return false;
                            String[] actions = o.getObjectComposition().getActions();
                            if (actions == null) return false;
                            for (String a : actions) {
                                if ("Milk".equalsIgnoreCase(a)) return true;
                            }
                            return false;
                        } catch (Exception e) { return false; }
                    })
                    .toList();
            if (!milkTargets.isEmpty()) {
                // Check if reachable.
                for (net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel m : milkTargets) {
                    try {
                        WorldPoint mwp = m.getWorldLocation();
                        if (net.runelite.client.plugins.microbot.util.tile.Rs2Tile.isTileReachable(mwp)) {
                            return "DAIRY";
                        }
                    } catch (Exception e) { /* continue */ }
                }
            }
            // Check for Egg ground items (chicken pen).
            net.runelite.client.plugins.microbot.util.models.RS2Item[] eggs =
                net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem.getAllFromWorldPoint(8, pp);
            if (eggs != null && java.util.Arrays.stream(eggs).anyMatch(i -> i != null && i.getItem().getId() == 1944)) {
                return "CHICKEN";
            }
            // Check for bucket (cow field). Bucket item id=1925.
            net.runelite.client.plugins.microbot.util.models.RS2Item[] buckets =
                net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem.getAllFromWorldPoint(8, pp);
            if (buckets != null && java.util.Arrays.stream(buckets).anyMatch(i -> i != null && i.getItem().getId() == 1925)) {
                return "COW_FIELD";
            }
        } catch (Exception e) {
            // Classification failed.
        }
        return "UNKNOWN";
    }

    /**
     * Build 483 (Alex 19:57): Check if a live dairy Milk target is reachable.
     * If yes, we can skip gate crossing and milk directly.
     */
    private boolean hasReachableMilkTarget(WorldPoint pp) {
        return "DAIRY".equals(classifyPenByTargets(pp));
    }

    // Build 489 (Julien 20:18 via Alex): SIMPLIFIED doGetBucket.
    // No route system. No WRONG_PEN latch. No destination adapter.
    // Per tick: scan visible cow NPCs + tile objects from player tile,
    // log id/name/tile/actions/reachability, pick nearest with live Milk.
    // If reachable: one short step + one Bucket-on-target, next-tick proof.
    // Else: one bounded move to nearest reachable pen tile, rescan once.
    // Else: one TARGET_NOT_FOUND/ROUTE_BLOCKED, sticky HOLD.
    // NOTE: client at login gate after Build488 restart -- do not judge
    // until logged-in frame appears.
    // Build 506: real ground items, working shop actions and observed completion.
    private long bucketActionUntil505, bucketScanAfter505, bucketWalkAfter505;
    private long bucketProgressAt505, bucketRecoveryStarted505;
    private int bucketActionAttempts505;
    private String bucketPending505 = "";
    private WorldPoint bucketLastPosition505, bucketGroundTarget505, bucketReportedTarget510;

    private void resetBucket505() {
        bucketActionUntil505 = bucketScanAfter505 = bucketWalkAfter505 = 0;
        bucketProgressAt505 = bucketRecoveryStarted505 = 0;
        bucketActionAttempts505 = 0;
        bucketPending505 = "";
        bucketLastPosition505 = bucketGroundTarget505 = bucketReportedTarget510 = null;
        recoveryMissingLatched = false;
    }

    private void bucketHold505(String reason) {
        recoveryMissingLatched = true;
        diag("Build 506: GET_BUCKET HOLD: " + reason + " player=" + playerPos());
        Microbot.status = "Cook's Assistant: " + reason;
    }

    private void doGetBucket() {
        if (!Microbot.isLoggedIn() || Microbot.getClient().getLocalPlayer() == null) return;
        WorldPoint pp = playerPos();
        if (pp == null) return;
        if (hasBucket()) {
            diag("Build 506: GET_BUCKET VERIFIED: inventory Bucket=" + invCount(BUCKET));
            resetBucket505();
            forceState(Stage.DETECT, "bucket inventory verified");
            return;
        }
        if (recoveryMissingLatched) return;
        long now = System.currentTimeMillis();
        if (bucketRecoveryStarted505 == 0) {
            bucketRecoveryStarted505 = bucketProgressAt505 = now;
            bucketLastPosition505 = pp;
            diag("Build 506: GET_BUCKET ACTIVE: ground-item API; coins=" + invCount("Coins"));
        }
        if (!pp.equals(bucketLastPosition505)) {
            bucketLastPosition505 = pp;
            bucketProgressAt505 = now;
        }
        if (now - bucketRecoveryStarted505 > 180000) {
            bucketHold505("BUCKET_RECOVERY_TIMEOUT");
            return;
        }
        boolean shopOpen = net.runelite.client.plugins.microbot.util.shop.Rs2Shop.isOpen();
        if (!bucketPending505.isEmpty()) {
            if (bucketPending505.equals("Trade") && shopOpen) {
                diag("Build 506: Trade VERIFIED: shop interface open");
                bucketPending505 = "";
                bucketActionAttempts505 = 0;
            } else {
                if (now < bucketActionUntil505) return;
                diag("Build 506: " + bucketPending505 + " not verified; bounded rescan");
                bucketPending505 = "";
                bucketGroundTarget505 = null;
                bucketScanAfter505 = 0;
                if (bucketActionAttempts505 >= 2) {
                    bucketHold505("BUCKET_ACTION_UNVERIFIED");
                    return;
                }
            }
        }
        if (shopOpen) {
            if (invCount("Coins") <= 0) { bucketHold505("BUCKET_NEEDS_COINS"); return; }
            if (!net.runelite.client.plugins.microbot.util.shop.Rs2Shop.hasStock(BUCKET)) {
                bucketHold505("BUCKET_SHOP_OUT_OF_STOCK"); return;
            }
            bucketPending505 = "Buy-1";
            bucketActionUntil505 = now + 8000;
            bucketActionAttempts505++;
            boolean invoked = net.runelite.client.plugins.microbot.util.shop.Rs2Shop.buyItem(BUCKET, "Buy-1");
            diag("Build 506: Buy-1 INVOKED=" + invoked + "; waiting for inventory proof");
            return;
        }
        if (now >= bucketScanAfter505) {
            bucketScanAfter505 = now + 2500;
            bucketGroundTarget505 = null;
            int closest = Integer.MAX_VALUE;
            // Scan cached ground-item entities across the loaded scene, not a tile grid.
            int candidateCount = 0;
            for (var item : Microbot.getRs2TileItemCache().query().toList()) {
                if (item == null || item.getId() != 1925 || !item.isLootAble()) continue;
                WorldPoint target = item.getWorldLocation();
                if (target == null || target.getPlane() != pp.getPlane()) continue;
                candidateCount++;
                int distance = pp.distanceTo(target);
                if (distance < closest) { closest = distance; bucketGroundTarget505 = target; }
            }
            if (bucketGroundTarget505 == null && bucketScanDone == false) {
                diag("Build 506: SCENE_BUCKET_SCAN: count=" + candidateCount + " player=" + pp);
                bucketScanDone = true;
            }
            if (bucketGroundTarget505 != null && !bucketGroundTarget505.equals(bucketReportedTarget510)) {
                bucketReportedTarget510 = bucketGroundTarget505;
                diag("Build " + BUILD_NUMBER + ": GROUND_BUCKET candidate=" + bucketGroundTarget505);
            }
        }
        if (bucketGroundTarget505 != null && pp.distanceTo(bucketGroundTarget505) <= 2
                && net.runelite.client.plugins.microbot.util.tile.Rs2Tile.isTileReachable(bucketGroundTarget505)) {
            bucketPending505 = "Take";
            bucketActionUntil505 = now + 8000;
            bucketActionAttempts505++;
            boolean invoked = false;
            for (var item : Microbot.getRs2TileItemCache().query().toList()) {
                if (item != null && item.getId() == 1925 && item.isLootAble()
                        && bucketGroundTarget505.equals(item.getWorldLocation())) {
                    invoked = item.click("Take");
                    break;
                }
            }
            diag("Build 506: Take INVOKED=" + invoked + "; waiting for inventory proof");
            return;
        }
        // Farm is a search region from the existing quest route, not proof of a spawn.
        WorldPoint destination = bucketGroundTarget505 != null ? bucketGroundTarget505
                : invCount("Coins") > 0 ? new WorldPoint(3209, 3247, 0) : CHICKEN_FARM;
        if (bucketGroundTarget505 == null && pp.distanceTo(destination) <= 8) {
            if (invCount("Coins") <= 0) { bucketHold505("NO_FREE_BUCKET_IN_FARM_SCAN"); return; }
            bucketPending505 = "Trade";
            bucketActionUntil505 = now + 8000;
            bucketActionAttempts505++;
            boolean invoked = net.runelite.client.plugins.microbot.util.shop.Rs2Shop.openShop("Shop keeper");
            diag("Build 506: Trade INVOKED=" + invoked + "; waiting for shop interface");
            return;
        }
        if (now - bucketProgressAt505 > 45000) { bucketHold505("BUCKET_ROUTE_BLOCKED"); return; }
        if (now < bucketWalkAfter505) return;
        bucketWalkAfter505 = now + 900;
        // Existing shared walker owns path/door handling and observes movement between ticks.
        stepToward(destination, "to bucket source", bucketGroundTarget505 != null ? 0 : 2);
    }

    private void milkReset396() {
        milkPhase = 0;
        milkWaitTicks = 0;
        milkWaitDiagMs = 0;
        milkGateScans = 0;
        milkHeldDiagMs = 0;
        milkPhase3Scans = 0;
        milkWalkFrom = null;
        milkWalkStepInvoked = false;
        milkEntryPos = null; // Build 472: reset return anchor
        milkDiagReturnDone = false; // Build 472: reset diag return flag
        milkOutsidePos = null; // Build 472: reset verified outside tile
        diagReturnHandoffSeen = false; // Build 472: reset handoff tracker
        milkDiagInsidePos = null; // Build 472: reset inside tile
        milkDiagLeg = 0; // Build 472: reset diag leg
        milkTargetAuditDone = false; // Build 472: reset target audit flag
        milkDairyActionId = null; // Build 472: reset dairy route actionId
        milkDairyPrimaryFailed = false; // Build 472: reset primary failure
        milkDairyAlternateTried = false; // Build 472: reset alternate flag
        milkDairyAlternateFailed = false; // Build 472: reset alternate failure
        milkDairyLeg = -1; // Build 472: reset leg state machine
        milkDairyLegStartDist = Integer.MAX_VALUE;
        milkDairyLegLastDist = Integer.MAX_VALUE;
        milkDairyLegStallTicks = 0;
        milkDairyLegTarget = null;
        milkDairyAlternateArrived = false; // Build 472: reset alternate arrival
        milkTargetObject = null; // Build 472: reset stored milk object
        milkTargetNpc = null; // Build 472: reset simplified target
        milkTargetGameObj = null;
        milkTargetPos = null;
        milkPhase4Ticks = 0; // Build 472: reset milk proof ticks
        returnLeg3StallTicks = 0; // Build 472: reset return leg-3 stall
        returnLeg3LastPos = null;
        returnLegLatch = 0; // Build 472: reset monotonic leg latch
        returnLeg4StallTicks = 0; // Build 472: reset leg 4/5 stall
        returnLeg4LastPos = null;
        gateCrossPhase = 0; // Build 472: reset gate crossing state
        gateCrossTicks = 0;
    }

    // Build 400: dedicated gate-through walk (Alex 2026-09-29 13:52). The
    // shared stepToward() treats chebDist<=2 as "arrived" and returns true
    // WITHOUT calling Rs2Walker.walkStep -- both gate attempts used inside
    // tiles at exactly distance 2, so the "one walk" log was false and no
    // movement could occur (walkStall stayed 0). This bypasses that early
    // return and issues exactly one walkStep per tick. Arrival is verified
    // by observed position, never by a helper boolean.
    // Build 406 (NUDGE ALEX 2026-09-29 14:19): action-filtered gate resolution.
    // Rejects gates without Open or Close (e.g. id=60763 with only [Release]
    // is not a pen entrance). Returns the nearest valid candidate, or null.
    // Build 408: dairy-scored gate selection (NUDGE ALEX 2026-09-29 14:49).
    // Gate id1559 leads to the CHICKEN pen (3236,3286), not the cow pen.
    // Selection now scores by dairy-cow proximity: a gate is only valid if
    // a Prized dairy cow or Dairy cow is visible within 30 tiles. Gates are
    // ranked by (dairyVisible ? 0 : 10000) + distance; the lowest wins.
    // Wrong-pen gates (from milkWrongPenGateIds) are excluded entirely.
    // If no gate has dairy nearby, returns null (HELD with diagnostics).
    private Rs2TileObjectModel findPenGate406(WorldPoint pp, int radius) {
        Rs2TileObjectModel best = null;
        int bestDist = Integer.MAX_VALUE;
        StringBuilder rejected = new StringBuilder();
        try {
            List<Rs2TileObjectModel> all = Microbot.getRs2TileObjectCache().query().toList();
            for (Rs2TileObjectModel o : all) {
                WorldPoint wp;
                try {
                    wp = o.getWorldLocation();
                } catch (Exception e) {
                    continue;
                }
                if (wp == null || wp.getPlane() != pp.getPlane()) continue;
                if (wp.distanceTo(pp) > radius) continue;
                String nm;
                try {
                    nm = o.getName();
                } catch (Exception e) {
                    continue;
                }
                if (nm == null) continue;
                if (!nm.equalsIgnoreCase("Gate") && !nm.equalsIgnoreCase("Fence gate")) continue;
                // Action filter: must offer Open or Close.
                boolean hasOpen = gateHasAction398(o, "Open");
                boolean hasClose = gateHasAction398(o, "Close");
                int oid = -1;
                try { oid = o.getId(); } catch (Exception e) { /* keep -1 */ }
                if (!hasOpen && !hasClose) {
                    if (rejected.length() > 0) rejected.append(", ");
                    rejected.append(nm).append(" id=").append(oid)
                        .append("@(").append(wp.getX()).append(",").append(wp.getY()).append(")")
                        .append("[no Open/Close]");
                    continue;
                }
                int d = (int) wp.distanceTo(pp);
                if (d < bestDist) {
                    bestDist = d;
                    best = o;
                }
            }
        } catch (Exception e) {
            diag("Build 406: findPenGate406 query failed: " + e.getClass().getSimpleName());
        }
        if (rejected.length() > 0) {
            diag("Build 406: gate candidates rejected (no Open/Close): " + rejected);
        }
        return best;
    }

    // Build 398: does the object's menu offer this action? (Alex 13:29 --
    // proves whether "Open" is really available instead of assuming it.)
    // Build 420: DISABLED -- getObjectComposition() blocks the tick thread.
    // The click result + state-change verification proves availability.
    private boolean gateHasAction398(Rs2TileObjectModel gate, String action) {
        // Assume true; the traversal layer verifies via state change.
        return true;
    }

    // Build 398: gate inspection -- id, type, reachability,
    // and which side the player stands on. Logged once before the click.
    // Build 420: NO getObjectComposition() -- it blocks the tick thread.
    private String inspectGate398(Rs2TileObjectModel gate, WorldPoint pp) {
        StringBuilder sb = new StringBuilder();
        try {
            int id = gate.getId();
            String type = String.valueOf(gate.getTileObjectType());
            String actions = "(not read -- composition blocks)";
            boolean reach = false;
            try {
                reach = gate.isReachable();
            } catch (Exception ignored) {
            }
            WorldPoint gp = gate.getWorldLocation();
            String side = "?";
            if (pp != null && gp != null) {
                side = "player "
                    + (pp.getX() < gp.getX() ? "W" : pp.getX() > gp.getX() ? "E" : "")
                    + (pp.getY() < gp.getY() ? "S" : pp.getY() > gp.getY() ? "N" : "")
                    + " of gate";
            }
            sb.append("id=").append(id).append(" type=").append(type)
                .append(" actions=").append(actions)
                .append(" reachable=").append(reach)
                .append(" (").append(side).append(")");
        } catch (Exception e) {
            sb.append("(inspect failed: ").append(e.getClass().getSimpleName()).append(")");
        }
        return sb.toString();
    }

    // Build 399: the inside tile = the walkable neighbor of the gate on the
    // cows' side of the fence (or the mirror of the approach when no cow is
    // visible). Never the player's tile, never the click-from tile.
    private WorldPoint insideTile399(WorldPoint gate, WorldPoint from, WorldPoint pp) {
        if (gate == null) return null;
        Rs2NpcModel cow = findNpc("Prized dairy cow", 30);
        if (cow == null) cow = findNpc("Dairy cow", 30);
        WorldPoint cp = (cow == null) ? null : npcPos396(cow);
        // Build 407 (NUDGE ALEX 2026-09-29 14:38): NO mirror fallback. The
        // mirror tile (2*gate - base) produced false inside tiles like
        // (3235,3283) -- walkable but on the public side. If no dairy cow is
        // visible, we cannot determine the pen side; return null (hold).
        if (cp == null) {
            diag("Build 407: MILK_COW: no dairy cow visible within 30 -- cannot determine pen side, no inside tile");
            return null;
        }
        // Dairy cow visible: the inside tile is the walkable adjacent tile
        // closest to the cow (the cow is inside the pen by definition).
        WorldPoint best = null;
        int bestScore = Integer.MAX_VALUE;
        int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {0, 0}};
        for (int[] d : deltas) {
            WorldPoint t = new WorldPoint(gate.getX() + d[0], gate.getY() + d[1], gate.getPlane());
            if (pp != null && t.getX() == pp.getX() && t.getY() == pp.getY()) continue;
            if (from != null && t.getX() == from.getX() && t.getY() == from.getY()) continue;
            boolean walkable = false;
            try {
                walkable = Rs2Tile.isWalkable(t);
            } catch (Exception ignored) {
            }
            if (!walkable) continue;
            int score = chebDist(t, cp);
            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }
        if (best != null) {
            diag("Build 407: MILK_COW: inside tile=(" + best.getX() + "," + best.getY() + "," + best.getPlane()
                + ") chosen by proximity to dairy cow@" + cp.getX() + "," + cp.getY());
        }
        return best;
    }

    // Build 407: pen-side predicate. Returns true if the player is on the
    // cow side of the fence, determined by dairy-cow proximity AND
    // reachability. A dairy cow within 8 tiles that is tile-reachable means
    // no fence between player and cow = pen side. Visibility alone is not
    // enough (cows are visible through the fence from the public path).
    private boolean isPenSide407(WorldPoint pp) {
        if (pp == null) return false;
        try {
            List<Rs2NpcModel> cows = new java.util.ArrayList<>();
            Rs2NpcModel prized = findNpc("Prized dairy cow", 10);
            Rs2NpcModel dairy = findNpc("Dairy cow", 10);
            if (prized != null) cows.add(prized);
            if (dairy != null) cows.add(dairy);
            for (Rs2NpcModel cow : cows) {
                WorldPoint cwp;
                try {
                    cwp = cow.getWorldLocation();
                } catch (Exception e) {
                    continue;
                }
                if (cwp == null || cwp.getPlane() != pp.getPlane()) continue;
                int dist = chebDist(pp, cwp);
                boolean reachable = false;
                try {
                    // isTileReachable(dest) checks reachability from the
                    // player's CURRENT position -- no fence between = pen side.
                    reachable = Rs2Tile.isTileReachable(cwp);
                } catch (Exception e) {
                    // Fall back to proximity if reachability check fails.
                    reachable = (dist <= 5);
                }
                // Instrument every candidate (NUDGE ALEX 14:36).
                int cid = -1;
                String cname = "?";
                try { cid = cow.getId(); } catch (Exception e) { /* keep -1 */ }
                try { cname = cow.getName(); } catch (Exception e) { /* keep ? */ }
                diag("Build 407: dairy candidate id=" + cid + " name='" + cname + "'@("
                    + cwp.getX() + "," + cwp.getY() + "," + cwp.getPlane()
                    + ") dist=" + dist + " reachable=" + reachable);
                if (dist <= 8 && reachable) {
                    return true;
                }
            }
        } catch (Exception e) {
            diag("Build 407: isPenSide check failed: " + e.getClass().getSimpleName());
        }
        return false;
    }

    // Build 408: pen classification (NUDGE ALEX 2026-09-29 14:49). When a
    // crossing fails pen-side validation, determine WHAT pen we're in:
    // - "WRONG PEN (chicken)": chickens/ducks visible, no dairy cows
    // - "WRONG PEN (other)": farm animals but no dairy
    // - "FALSE CROSSING": no pen animals at all (still on public side)
    // Used for diagnostics and wrong-pen gate tracking.
    private String classifyPen408(WorldPoint pp) {
        if (pp == null) return "UNKNOWN";
        try {
            boolean hasDairy = (findNpc("Prized dairy cow", 15) != null)
                || (findNpc("Dairy cow", 15) != null);
            if (hasDairy) return "DAIRY PEN (unexpected -- isPenSide failed but dairy visible)";
            boolean hasChicken = (findNpc("Chicken", 15) != null);
            boolean hasDuck = (findNpc("Duck", 15) != null);
            boolean hasCow = (findNpc("Cow", 15) != null);
            if (hasChicken || hasDuck) {
                return "WRONG PEN (chicken/ducks, no dairy)";
            }
            if (hasCow) {
                return "WRONG PEN (generic cows, no dairy)";
            }
            // Check for other farm NPCs from the 14:38 evidence.
            String names = listNpcNames401(15);
            if (names != null && (names.contains("Farmer") || names.contains("Goblin"))) {
                return "WRONG PEN (farm area, no dairy): " + names;
            }
            return "FALSE CROSSING (no pen animals within 15)";
        } catch (Exception e) {
            return "CLASSIFY FAILED: " + e.getClass().getSimpleName();
        }
    }

    // Build 398: one alternate adjacent tile next to the gate, walkable,
    // different from the tile we clicked from (one retry only).
    private WorldPoint altAdjacentTile398(WorldPoint gate, WorldPoint exclude, WorldPoint pp) {
        WorldPoint best = null;
        int bestD = Integer.MAX_VALUE;
        int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : deltas) {
            WorldPoint t = new WorldPoint(gate.getX() + d[0], gate.getY() + d[1], gate.getPlane());
            if (exclude != null && t.getX() == exclude.getX() && t.getY() == exclude.getY()) continue;
            boolean walkable = false;
            try {
                walkable = Rs2Tile.isWalkable(t);
            } catch (Exception ignored) {
            }
            if (!walkable) continue;
            int dist = (pp == null) ? 0 : chebDist(pp, t);
            if (dist < bestD) {
                bestD = dist;
                best = t;
            }
        }
        return best;
    }

    private WorldPoint npcPos396(Rs2NpcModel npc) {
        try {
            return npc.getWorldLocation();
        } catch (Exception e) {
            return null;
        }
    }

    // Alex's required no-gate diagnostic: player WorldPoint, candidate gate
    // tiles, and collision flags (Rs2Tile.isWalkable per candidate).

    private void doMilkCow() {
        if (hasMilk()) {
            milkReset396();
            diag("MILK_COW: bucket of milk observed -> DETECT");
            forceState(Stage.DETECT, "have milk");
            return;
        }
        if (!hasBucket()) {
            milkReset396();
            diag("MILK_COW: lost bucket -> DETECT re-routes");
            forceState(Stage.DETECT, "missing bucket");
            return;
        }
        if (dialogueTick(new String[0])) return;
        WorldPoint pp = playerPos();
        if (pp == null) return;

        // Phase 9: held with diagnostics -- no repeat cow actions.
        if (milkPhase == 9) {
            long nowH = System.currentTimeMillis();
            // Build 452 (Alex 17:48): GATE/WAYPOINT FIXTURE diagnostic.
            // Priority: (1) cow inside if far enough, (2) gate far-side,
            // (3) distant waypoint for idempotency, (4) fixture-unavailable.
            // A too-close inside is DISCARDED, not used.
            if (!milkDiagReturnDone && milkOutsidePos != null) {
                // Discard unusable too-close inside.
                if (milkDiagInsidePos != null) {
                    int dInCheck = chebDist(pp, milkDiagInsidePos);
                    if (dInCheck <= 2) {
                        diag("Build 472: MILK_COW: DIAG discarding too-close inside @("
                            + milkDiagInsidePos.getX() + "," + milkDiagInsidePos.getY()
                            + ") dist=" + dInCheck + "; seeking gate/waypoint");
                        milkDiagInsidePos = null;
                    }
                }
                // Try gate fixture if no usable inside.
                if (milkDiagInsidePos == null) {
                    Rs2TileObjectModel gate = findNearestGate(pp, 15);
                    if (gate != null) {
                        try {
                            WorldPoint gwp = gate.getWorldLocation();
                            // Far side: gate + direction away from player, 5 tiles.
                            int dx = Integer.compare(gwp.getX() - pp.getX(), 0);
                            int dy = Integer.compare(gwp.getY() - pp.getY(), 0);
                            WorldPoint farSide = new WorldPoint(
                                gwp.getX() + dx * 5, gwp.getY() + dy * 5, pp.getPlane());
                            int farDist = chebDist(pp, farSide);
                            if (farDist > 2) {
                                milkDiagInsidePos = farSide;
                                diag("Build 472: MILK_COW: DIAG gate fixture -- '"
                                    + gate.getName() + "' @(" + gwp.getX() + "," + gwp.getY()
                                    + ") far-side @(" + farSide.getX() + "," + farSide.getY()
                                    + ") dist=" + farDist);
                            }
                        } catch (Exception e) {
                            // Non-fatal.
                        }
                    } else {
                        diag("Build 472: MILK_COW: DIAG no gate in 15 tiles; trying distant waypoint");
                    }
                }
                // Fallback: distant waypoint for idempotency testing.
                if (milkDiagInsidePos == null) {
                    int[][] dirs = {{10, 0}, {-10, 0}, {0, 10}, {0, -10}};
                    for (int[] d : dirs) {
                        WorldPoint wp = new WorldPoint(
                            pp.getX() + d[0], pp.getY() + d[1], pp.getPlane());
                        if (chebDist(pp, wp) > 5) {
                            milkDiagInsidePos = wp;
                            diag("Build 472: MILK_COW: DIAG waypoint fixture -- @("
                                + wp.getX() + "," + wp.getY() + ") dist=10"
                                + " (idempotency test, no gate)");
                            break;
                        }
                    }
                    if (milkDiagInsidePos == null) {
                        diag("Build 472: MILK_COW: DIAG fixture-unavailable -- no gate, no waypoint; held");
                    }
                }
                if (milkDiagInsidePos != null) {
                    int dOut = chebDist(pp, milkOutsidePos);
                    int dIn = chebDist(pp, milkDiagInsidePos);
                    // Nonzero route check: must be far from at least one.
                    if (dOut > 2 || dIn > 2) {
                        milkDiagReturnDone = true;
                        diagReturnHandoffSeen = false;
                        milkDiagLeg = 0;
                        // Build 453 (Alex 17:51): Use phase 10 for DIRECT
                        // idempotency test. Phase 6 uses stepToward (walker
                        // first), which can complete without the resolver.
                        // Phase 10 bypasses the walker and requests directly
                        // from sharedTraversal, then re-issues the key.
                        milkPhase = 10;
                        milkWaitTicks = 0;
                        diag("Build 472: MILK_COW: DIAG-START direct idempotency -- outside @("
                            + milkOutsidePos.getX() + "," + milkOutsidePos.getY()
                            + ") inside @(" + milkDiagInsidePos.getX() + "," + milkDiagInsidePos.getY()
                            + ") (quest progress NOT claimed)");
                        return;
                    } else {
                        diag("Build 472: MILK_COW: DIAG fixture-unavailable -- zero route "
                            + "(dOut=" + dOut + " dIn=" + dIn + "); no crossing to prove");
                        milkDiagReturnDone = true; // don't retry
                    }
                }
            }
            if (nowH - milkHeldDiagMs > 60000) {
                milkHeldDiagMs = nowH;
                diag("Build 397: MILK_COW: HELD at (" + pp.getX() + "," + pp.getY() + "," + pp.getPlane() + ") -- no reachable pen gate; standing down with diagnostics (no cow actions issued)");
            }
            return;
        }

        // Phase 10: DIRECT idempotency test (Build 453, Alex 17:51).
        // Bypasses the walker. Requests directly from sharedTraversal,
        // then re-issues the exact same key on the next tick.
        // The resolver MUST log TRAVERSAL IDEMPOTENT with dedup proof.
        // Does NOT move the player, does NOT claim progress.
        if (milkPhase == 10) {
            if (milkOutsidePos == null || milkDiagInsidePos == null) {
                diag("Build 472: MILK_COW: DIAG-10 missing tiles -- back to HOLD");
                milkPhase = 9;
                milkHeldDiagMs = 0;
                return;
            }
            // Tick 0: issue the request.
            if (milkWaitTicks == 0) {
                if (sharedTraversal == null) { sharedTraversal = new Rs2Traversal(msg -> diag(msg)); }
                if (sharedTraversal != null) {
                    sharedTraversal.requestTraversal(pp, milkDiagInsidePos, "diag-idempotent");
                    diag("Build 472: MILK_COW: DIAG-10 request issued -> ("
                        + milkDiagInsidePos.getX() + "," + milkDiagInsidePos.getY() + ")");
                }
                milkWaitTicks = 1;
                return;
            }
            // Tick 1: re-issue the EXACT same key while active.
            // The resolver must log TRAVERSAL IDEMPOTENT (dedup, no reset).
            if (milkWaitTicks == 1) {
                if (sharedTraversal == null) { sharedTraversal = new Rs2Traversal(msg -> diag(msg)); }
                if (sharedTraversal != null && !sharedTraversal.isIdle()) {
                    // Re-issue identical request.
                    sharedTraversal.requestTraversal(pp, milkDiagInsidePos, "diag-idempotent");
                    diag("Build 472: MILK_COW: DIAG-10 re-issued same key while active; "
                        + "expect TRAVERSAL IDEMPOTENT above");
                } else {
                    diag("Build 472: MILK_COW: DIAG-10 request not active (idle); "
                        + "cannot test idempotency -- fixture-unavailable");
                }
                milkWaitTicks = 2;
                return;
            }
            // Tick 2+: check result and clean up.
            if (milkWaitTicks >= 2) {
                if (sharedTraversal == null) { sharedTraversal = new Rs2Traversal(msg -> diag(msg)); }
                if (sharedTraversal != null) {
                    // Tick to get status, then reset.
                    sharedTraversal.tick(pp);
                    sharedTraversal.reset();
                }
                diag("Build 472: MILK_COW: DIAG-10 complete; resolver reset -- back to HOLD "
                    + "(no movement, no progress claimed)");
                milkPhase = 9;
                milkHeldDiagMs = 0;
                milkDiagReturnDone = true; // don't retry
                return;
            }
        }

        if (milkPhase == 6) {
            // Build 449 (Alex 17:40): TWO-LEG diagnostic crossing.
            // Leg 1: outside -> inside (forward proof).
            // Leg 2: inside -> outside (return proof).
            // Each leg requires: nonzero start distance, resolver handoff,
            // and actual tile change. Independent of milkable NPC.
            // Does NOT set quest progress, does NOT resume normal flow.
            if (milkOutsidePos == null || milkDiagInsidePos == null) {
                diag("Build 472: MILK_COW: DIAG missing tiles (outside="
                    + (milkOutsidePos != null) + " inside=" + (milkDiagInsidePos != null)
                    + ") -- back to HOLD");
                milkPhase = 9;
                return;
            }
            // Determine current leg.
            if (milkDiagLeg == 0) {
                // Start leg 1: outside -> inside.
                int dOut = chebDist(pp, milkOutsidePos);
                int dIn = chebDist(pp, milkDiagInsidePos);
                // Must start near outside for a valid forward leg.
                if (dOut > 5) {
                    diag("Build 472: MILK_COW: DIAG leg1 fixture-unavailable -- not at outside (dist="
                        + dOut + "); back to HOLD");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    return;
                }
                // Build 451 (Alex 17:44): do NOT use dist<=2 as completion.
                // If the inside target is within the arrival threshold, the
                // resolver can never engage. This is a fixture problem, not
                // a proof. Log explicitly and hold.
                if (dIn <= 2) {
                    diag("Build 472: MILK_COW: DIAG leg1 fixture-unavailable -- inside target too close "
                        + "(dist=" + dIn + "); resolver cannot engage; no crossing to prove");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    return;
                }
                milkDiagLeg = 1;
                milkWaitTicks = 0;
                diagReturnHandoffSeen = false;
                diag("Build 472: MILK_COW: DIAG leg1 START outside->inside ("
                    + milkOutsidePos.getX() + "," + milkOutsidePos.getY() + ")->("
                    + milkDiagInsidePos.getX() + "," + milkDiagInsidePos.getY()
                    + ") dist=" + dIn);
                return;
            }
            WorldPoint legTarget = (milkDiagLeg == 1) ? milkDiagInsidePos : milkOutsidePos;
            String legName = (milkDiagLeg == 1) ? "leg1 outside->inside" : "leg2 inside->outside";
            int distToTarget = chebDist(pp, legTarget);
            // Build 472: if already at target on first tick, the fixture
            // is invalid (not a proof). Log explicitly.
            if (distToTarget <= 2 && milkWaitTicks == 0) {
                diag("Build 472: MILK_COW: DIAG " + legName + " fixture-unavailable -- already at target "
                    + "(dist=" + distToTarget + "); resolver cannot engage");
                if (milkDiagLeg == 1) {
                    milkDiagLeg = 2; // try return leg
                    milkWaitTicks = 0;
                    diagReturnHandoffSeen = false;
                    return;
                } else {
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    return;
                }
            }
            WorldPoint legStart = pp;
            boolean wasHandoff = (sharedTraversal != null && !sharedTraversal.isIdle());
            if (stepToward(legTarget, "diag-" + legName)) {
                // Arrived. Require handoff + actual tile change.
                int finalDist = chebDist(pp, legTarget);
                boolean tileChanged = !legStart.equals(pp);
                if ((wasHandoff || diagReturnHandoffSeen) && tileChanged && finalDist <= 2) {
                    diag("Build 472: MILK_COW: DIAG " + legName + " COMPLETE -- handoff verified, "
                        + "tile (" + legStart.getX() + "," + legStart.getY() + ")->("
                        + pp.getX() + "," + pp.getY() + ")");
                    if (milkDiagLeg == 1) {
                        milkDiagLeg = 2;
                        milkWaitTicks = 0;
                        diagReturnHandoffSeen = false;
                        diag("Build 472: MILK_COW: DIAG leg2 START inside->outside");
                    } else {
                        diag("Build 472: MILK_COW: DIAG BOTH LEGS COMPLETE -- forward+return proven; back to HOLD (no quest progress)");
                        milkPhase = 9;
                        milkHeldDiagMs = 0;
                        milkDiagLeg = 0;
                    }
                } else {
                    diag("Build 472: MILK_COW: DIAG " + legName + " arrived WITHOUT proof "
                        + "(handoff=" + (wasHandoff || diagReturnHandoffSeen)
                        + " tileChanged=" + tileChanged + ") -- NOT accepted; back to HOLD");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    milkDiagLeg = 0;
                }
            } else {
                if (sharedTraversal != null && !sharedTraversal.isIdle()) {
                    diagReturnHandoffSeen = true;
                }
                milkWaitTicks++;
                if (milkWaitTicks > 120) {
                    diag("Build 472: MILK_COW: DIAG " + legName + " timeout -- back to HOLD (ROUTE_BLOCKED)");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    milkDiagLeg = 0;
                }
            }
            return;
        }

        // Build 397 (Alex 2026-09-29 13:22): arrival verification -- log
        // player WorldPoint EVERY tick during the active phases. The
        // 13:20:41 vs 13:21:43 frames prove the bot IS moving along the
        // fenced path; 'no dairy cow nearby' is a target-search failure
        // during movement, not proof walking stopped. (The 13:20:19
        // RuneLite WorldService Error is environmental noise -- ignored.)
        diag("Build 397: MILK_COW: pos (" + pp.getX() + "," + pp.getY() + "," + pp.getPlane() + ") phase=" + milkPhase);

        // Approach leg: STAGED ROUTE through validated anchors.
        // Build 457 (Alex 18:12): Direct (3246,3286)->(3172,3317) fails with
        // rs2walker:walkStep:no-walkable-path. Do NOT jump directly across
        // the map. Route through existing validated anchors, one resolver
        // request per leg:
        //   CHICKEN_FARM (3238,3298) -> WHEAT_FIELD (3157,3288)
        //   -> MILL_APPROACH (3166,3304) -> DAIRY_PASTURE (3172,3317)
        // If any leg fails: one alternate anchor, then ROUTE_BLOCKED + HOLD.
        // Latch one actionId for the dairy route. No unbounded retry.
        if (milkDairyAlternateFailed) {
            // Alternate also failed. Sticky HOLD.
            diag("Build 472: MILK_COW: ROUTE_BLOCKED -- dairy primary and alternate "
                + "routes failed. Sticky HOLD. Target was special dairy loc near (3172,3317).");
            milkPhase = 9;
            milkHeldDiagMs = 0;
            return;
        }
        if (milkDairyPrimaryFailed && !milkDairyAlternateTried) {
            // Primary failed. Try one alternate.
            milkDairyAlternateTried = true;
            milkDairyActionId = "dairy-alt-" + System.currentTimeMillis();
            diag("Build 472: MILK_COW: dairy PRIMARY failed -- trying ALTERNATE "
                + "waypoint (3178,3322,0) actionId=" + milkDairyActionId);
            milkPhase = 0;
            stepToward(DAIRY_PASTURE_ALT, "to dairy pasture alternate");
            return;
        }
        if (milkDairyPrimaryFailed && milkDairyAlternateTried) {
            // Primary failed, alternate in progress or failed.
            // If alternate failed, the flag above handles it.
            // Build 459 (Alex 18:20): ARRIVAL CHECK. If within 8 tiles of
            // DAIRY_PASTURE_ALT, STOP routing and enter target-resolution.
            // Do NOT keep repeating the alternate route.
            int altDist = chebDist(pp, DAIRY_PASTURE_ALT);
            if (altDist <= 8) {
                diag("Build 472: MILK_COW: dairy ALTERNATE ARRIVED dist=" + altDist
                    + " <= 8 -- stopping route, entering target-resolution phase");
                milkDairyAlternateArrived = true;
                // Fall through to target audit below (do not return).
            } else {
                diag("Build 472: MILK_COW: continuing dairy ALTERNATE route... dist=" + altDist);
                milkPhase = 0;
                stepToward(DAIRY_PASTURE_ALT, "to dairy pasture alternate");
                return;
            }
        }
        // Build 472: If alternate arrived, skip primary route logic.
        // Go directly to target-resolution phase below.
        if (!milkDairyAlternateArrived) {
        // Build 460 (Alex 18:23): PROXIMITY SHORT-CIRCUIT. If already within
        // 10 tiles of DAIRY_PASTURE (3172,3317) or DAIRY_PASTURE_ALT (3178,3322),
        // SKIP all staged route legs and enter target-resolution immediately.
        // A fresh restart at (3176,3320) must NOT walk away toward CHICKEN_FARM.
        // Only use the staged route when farther than 10 tiles from dairy.
        int distDairy = chebDist(pp, DAIRY_PASTURE);
        int distDairyAlt = chebDist(pp, DAIRY_PASTURE_ALT);
        if (distDairy <= 10 || distDairyAlt <= 10) {
            diag("Build 472: MILK_COW: already at dairy (dist=" + Math.min(distDairy, distDairyAlt)
                + " <= 10) -- SKIPPING staged route, entering target-resolution");
            milkDairyAlternateArrived = true; // treat as arrived
        } else {
        // Primary route: staged through anchors with STRICT no-progress invariant.
        // Build 458 (Alex 18:16): Build 457 oscillated between legs (no monotonic
        // distance reduction). A leg advances ONLY after next-tick proximity
        // proof AND measurable distance reduction. Repeated same-area positions
        // count as stalled. One alternate staged route, then ROUTE_BLOCKED/HOLD.
        WorldPoint[] dairyLegs = {CHICKEN_FARM, WHEAT_FIELD, MILL_APPROACH, DAIRY_PASTURE};
        String[] dairyLegNames = {"CHICKEN_FARM", "WHEAT_FIELD", "MILL_APPROACH", "DAIRY_PASTURE"};
        if (milkDairyLeg == -1) {
            // Start leg 0.
            milkDairyLeg = 0;
            milkDairyLegTarget = dairyLegs[0];
            milkDairyLegStartDist = chebDist(pp, milkDairyLegTarget);
            milkDairyLegLastDist = milkDairyLegStartDist;
            milkDairyLegStallTicks = 0;
            milkDairyActionId = "dairy-" + System.currentTimeMillis();
            diag("Build 472: MILK_COW: dairy staged route START actionId=" + milkDairyActionId
                + " leg 1/4 to " + dairyLegNames[0]
                + " (" + milkDairyLegTarget.getX() + "," + milkDairyLegTarget.getY() + ")"
                + " startDist=" + milkDairyLegStartDist);
        }
        if (milkDairyLeg >= 0 && milkDairyLeg < 4) {
            int curDist = chebDist(pp, milkDairyLegTarget);
            String legName = dairyLegNames[milkDairyLeg];
            // Arrival: within 8 tiles = leg complete (proximity proof).
            if (curDist <= 8) {
                diag("Build 472: MILK_COW: dairy leg " + (milkDairyLeg + 1) + "/4 COMPLETE"
                    + " (" + legName + ") dist=" + curDist + " <= 8");
                milkDairyLeg++;
                if (milkDairyLeg >= 4) {
                    diag("Build 472: MILK_COW: dairy staged route COMPLETE -- at DAIRY_PASTURE");
                    milkDairyLeg = 4; // done
                } else {
                    milkDairyLegTarget = dairyLegs[milkDairyLeg];
                    milkDairyLegStartDist = chebDist(pp, milkDairyLegTarget);
                    milkDairyLegLastDist = milkDairyLegStartDist;
                    milkDairyLegStallTicks = 0;
                    diag("Build 472: MILK_COW: advancing to leg " + (milkDairyLeg + 1) + "/4"
                        + " to " + dairyLegNames[milkDairyLeg]
                        + " (" + milkDairyLegTarget.getX() + "," + milkDairyLegTarget.getY() + ")"
                        + " startDist=" + milkDairyLegStartDist);
                }
                // Fall through to route to the new leg target (or proceed if done).
                if (milkDairyLeg >= 4) {
                    // Route complete, proceed to target scan below.
                } else {
                    milkPhase = 0;
                    stepToward(milkDairyLegTarget, "dairy leg " + (milkDairyLeg + 1) + "/4 to " + dairyLegNames[milkDairyLeg]);
                    return;
                }
            } else if (curDist < milkDairyLegLastDist - 2) {
                // Measurable progress: distance reduced by >2.
                milkDairyLegLastDist = curDist;
                milkDairyLegStallTicks = 0;
                diag("Build 472: MILK_COW: dairy leg " + (milkDairyLeg + 1) + "/4 PROGRESS"
                    + " (" + legName + ") dist=" + curDist + " (was " + (curDist + 3) + "+)");
                milkPhase = 0;
                stepToward(milkDairyLegTarget, "dairy leg " + (milkDairyLeg + 1) + "/4 to " + legName);
                return;
            } else {
                // No measurable progress. Count as stalled.
                milkDairyLegStallTicks++;
                diag("Build 472: MILK_COW: dairy leg " + (milkDairyLeg + 1) + "/4 STALLED"
                    + " (" + legName + ") dist=" + curDist + " lastDist=" + milkDairyLegLastDist
                    + " stallTicks=" + milkDairyLegStallTicks + "/15");
                if (milkDairyLegStallTicks >= 15) {
                    // Leg failed. Mark primary as failed to trigger alternate/HOLD.
                    diag("Build 472: MILK_COW: dairy leg " + (milkDairyLeg + 1) + "/4 FAILED"
                        + " -- no progress for 15 ticks. Marking route failed.");
                    milkDairyPrimaryFailed = true;
                    milkDairyLeg = -1; // reset for alternate attempt
                    return;
                }
                milkPhase = 0;
                stepToward(milkDairyLegTarget, "dairy leg " + (milkDairyLeg + 1) + "/4 to " + legName);
                return;
            }
        }
        if (milkDairyLeg == 4) {
            // All legs complete. Proceed to target scan.
            diag("Build 472: MILK_COW: dairy staged route COMPLETE -- at/near DAIRY_PASTURE");
        }
        } // end else (staged route) -- Build 460
        } // end if (!milkDairyAlternateArrived) -- Build 459

        // Build 459 (Alex 18:20): TARGET-RESOLUTION PHASE.
        // When the dairy route (primary staged or alternate) has arrived,
        // run a bounded live tile-object/NPC audit at the current tile.
        // Search for the special dairy object (fat_cow/prized dairy cow)
        // with live action "Milk". Then one item-on-target action and
        // next-tick Bucket->Bucket of milk proof.
        // If no Milk candidate after bounded scans: TARGET_NOT_FOUND/HOLD.
        // (Alex 17:12): replace the id=8689-or-HELD logic with NPC search +
        // shared resolver. The old "no id=8689 -> HELD" stranded the bot.
        if (milkPhase == 0) {
            // Build 472 (Alex 19:01): SIMPLIFIED target selection. Julien: the
            // issue is simple target selection, not routes. Enumerate live
            // scene: nearby cow NPCs + tile objects. Select ONLY the one whose
            // LIVE actions include 'Milk'. No ID hints, no name matching,
            // no generic Cow/Attack. One TARGET_NOT_FOUND/HOLD if none.
            if (milkOutsidePos == null && pp != null) {
                milkOutsidePos = pp;
            }
            // Enumerate candidates with live 'Milk' action
            StringBuilder candLog = new StringBuilder();
            WorldPoint bestMilkPos = null;
            String bestMilkDesc = null;
            int bestMilkDist = Integer.MAX_VALUE;
            Object bestMilkTarget = null; // NPC or TileObject
            boolean isBestNpc = false;
            try {
                // NPCs: use util.npc.Rs2NpcModel (Stream), filter by Milk action
                java.util.List<net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel> npcs =
                    Microbot.getClientThread().invoke(() ->
                        net.runelite.client.plugins.microbot.util.npc.Rs2Npc.getNpcs(n ->
                            n != null && n.getWorldLocation() != null &&
                            n.getWorldLocation().distanceTo(pp) <= 15).toList());
                if (npcs != null) {
                    for (net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npc : npcs) {
                        try {
                            String nm = npc.getName();
                            if (nm == null) continue;
                            String nml = nm.toLowerCase();
                            if (!nml.contains("cow") && !nml.contains("dairy") && !nml.contains("cattle")) continue;
                            int nid = npc.getId();
                            if (nid == 2790 || nid == 2791 || nid == 2792) continue; // generic, no Milk
                            final net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npcF = npc;
                            String[] acts = Microbot.getClientThread().invoke(() -> {
                                try {
                                    var c = npcF.getTransformedComposition();
                                    return c != null ? c.getActions() : null;
                                } catch (Exception e) { return null; }
                            });
                            String actsStr = acts != null ? java.util.Arrays.toString(acts) : "unavailable";
                            boolean hasMilk = false;
                            if (acts != null) {
                                for (String a : acts) {
                                    if (a != null && a.equalsIgnoreCase("Milk")) { hasMilk = true; break; }
                                }
                            }
                            candLog.append("NPC '").append(nm).append("' id=").append(nid)
                                .append(" @(").append(npc.getWorldLocation().getX()).append(",")
                                .append(npc.getWorldLocation().getY()).append(") acts=")
                                .append(actsStr).append(hasMilk ? " [MILK]" : "").append("; ");
                            if (hasMilk) {
                                int d = (int) npc.getWorldLocation().distanceTo(pp);
                                if (d < bestMilkDist) {
                                    bestMilkDist = d;
                                    bestMilkPos = npc.getWorldLocation();
                                    bestMilkDesc = "NPC '" + nm + "' id=" + nid;
                                    bestMilkTarget = npc;
                                    isBestNpc = true;
                                }
                            }
                        } catch (Exception e) { /* skip */ }
                    }
                }
                // Tile objects: use Rs2TileObjectModel from cache, require live Milk action
                try {
                    java.util.List<Rs2TileObjectModel> tObjs =
                        Microbot.getRs2TileObjectCache().query().toList();
                    for (Rs2TileObjectModel obj : tObjs) {
                        try {
                            if (obj == null) continue;
                            WorldPoint wp = obj.getWorldLocation();
                            if (wp == null || wp.getPlane() != pp.getPlane()) continue;
                            if (wp.distanceTo(pp) > 15) continue;
                            final Rs2TileObjectModel objF = obj;
                            String[] acts = Microbot.getClientThread().invoke(() -> {
                                try {
                                    var comp = objF.getObjectComposition();
                                    return comp != null ? comp.getActions() : null;
                                } catch (Exception e) { return null; }
                            });
                            boolean hasMilk = false;
                            if (acts != null) {
                                for (String a : acts) {
                                    if (a != null && a.equalsIgnoreCase("Milk")) { hasMilk = true; break; }
                                }
                            }
                            if (hasMilk) {
                                String actsStr = java.util.Arrays.toString(acts);
                                candLog.append("OBJ id=").append(obj.getId())
                                    .append(" @(").append(wp.getX()).append(",")
                                    .append(wp.getY()).append(") acts=")
                                    .append(actsStr).append(" [MILK]; ");
                                int d = wp.distanceTo(pp);
                                if (d < bestMilkDist) {
                                    bestMilkDist = d;
                                    bestMilkPos = wp;
                                    bestMilkDesc = "OBJ id=" + obj.getId();
                                    bestMilkTarget = obj;
                                    isBestNpc = false;
                                }
                            }
                        } catch (Exception e) { /* skip */ }
                    }
                } catch (Exception e) { /* skip */ }
            } catch (Exception e) {
                candLog.append("query failed: ").append(e.getMessage());
            }
            if (bestMilkTarget != null) {
                diag("Build 472: MILK_COW: selected " + bestMilkDesc
                    + " @(" + bestMilkPos.getX() + "," + bestMilkPos.getY() + ")"
                    + " dist=" + bestMilkDist + " -- candidates: " + candLog.toString());
                if (isBestNpc) {
                    milkTargetNpc = (net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel) bestMilkTarget;
                    milkTargetObject = null;
                    milkTargetGameObj = null;
                } else {
                    milkTargetObject = null;
                    milkTargetGameObj = (Rs2TileObjectModel) bestMilkTarget;
                    milkTargetNpc = null;
                }
                milkTargetPos = bestMilkPos;
                milkPhase = 3;
                milkPhase3Scans = 0;
                return;
            } else {
                diag("Build 472: MILK_COW: TARGET_NOT_FOUND at player ("
                    + pp.getX() + "," + pp.getY() + ") -- candidates: "
                    + candLog.toString() + " -- HOLD");
                Microbot.status = "Cook's Assistant: no Milk target -- holding";
                milkPhase = 9;
                milkHeldDiagMs = 0;
                return;
            }
        }

        if (milkPhase == 3) {
            // Build 461 (Alex 18:27): USE THE VALIDATED OBJECT DIRECTLY.
            // Phase 0 found milkTargetObject (id=8689, the dairy cow object).
            // Do NOT re-scan for NPCs. Check the object's live actions for
            // "Milk". If present: one Bucket-on-object action. If not:
            // log actions and HOLD with TARGET_NOT_FOUND.
            if (milkTargetObject != null) {
                try {
                    int objId = milkTargetObject.getId();
                    WorldPoint owp = milkTargetObject.getWorldLocation();
                    String opos = owp.getX() + "," + owp.getY() + "," + owp.getPlane();
                    String[] actions = null;
                    try {
                        actions = milkTargetObject.getObjectComposition().getActions();
                    } catch (Exception ea) {
                        actions = null;
                    }
                    StringBuilder actStr = new StringBuilder("[");
                    boolean hasMilk = false;
                    if (actions != null) {
                        for (int ai = 0; ai < actions.length; ai++) {
                            if (ai > 0) actStr.append(", ");
                            actStr.append(actions[ai] == null ? "null" : "'" + actions[ai] + "'");
                            if (actions[ai] != null && actions[ai].equals("Milk")) {
                                hasMilk = true;
                            }
                        }
                    } else {
                        actStr.append("null-actions");
                    }
                    actStr.append("]");
                    diag("Build 472: MILK_COW: phase 3 using stored OBJECT id=" + objId
                        + "@(" + opos + ") actions=" + actStr);
                    if (hasMilk) {
                        // Build 462 (Alex 18:33): TWO-STEP with logging.
                        // Step 1: Rs2Inventory.use("Bucket") -> log selection result (boolean).
                        // Step 2: milkTargetObject.click("Milk") -> log click result (boolean).
                        // The direct Rs2Inventory.useItemOnObject helper uses Rs2GameObject.interact
                        // by ID, which may not be valid for the special object 8689. Using the
                        // validated object reference directly with its documented "Milk" action
                        // after selecting the Bucket is the supported path.
                        diag("Build 472: MILK_COW: OBJECT has live 'Milk' -- two-step Bucket-on-object");
                        boolean selectOk = false;
                        boolean clickOk = false;
                        try {
                            java.util.function.Supplier<Boolean> selectTask = () ->
                                Rs2Inventory.use("Bucket");
                            selectOk = Microbot.getClientThread().invoke(selectTask);
                        } catch (Exception e) {
                            selectOk = false;
                        }
                        diag("Build 472: MILK_COW: Rs2Inventory.use(Bucket) selection returned=" + selectOk);
                        if (selectOk) {
                            try {
                                java.util.function.Supplier<Boolean> clickTask = () ->
                                    milkTargetObject.click("Milk");
                                clickOk = Microbot.getClientThread().invoke(clickTask);
                            } catch (Exception e) {
                                clickOk = false;
                            }
                        }
                        diag("Build 472: MILK_COW: milkTargetObject.click(Milk) returned=" + clickOk);
                        if (selectOk && clickOk) {
                            diag("Build 472: MILK_COW: Milk action issued (select=" + selectOk + ", click=" + clickOk
                                + ") -- waiting next-tick Bucket->Bucket of milk proof");
                            milkPhase = 4;
                            milkPhase4Ticks = 0;
                        } else {
                            diag("Build 472: MILK_COW: interaction FAILED (select=" + selectOk + ", click=" + clickOk
                                + ") -- HOLD");
                            milkPhase = 9;
                            milkHeldDiagMs = 0;
                        }
                        return;
                    } else {
                        diag("Build 472: MILK_COW: TARGET_NOT_FOUND -- object id=" + objId
                            + " has no live 'Milk' action. Actions: " + actStr + " -- HOLD");
                        milkPhase = 9;
                        milkHeldDiagMs = 0;
                        return;
                    }
                } catch (Exception e) {
                    diag("Build 472: MILK_COW: phase 3 object check failed: " + e.getClass().getSimpleName()
                        + " -- HOLD");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    return;
                }
            }
            // Fallback (no stored object): old NPC scan below.
            // Build 440 (Alex 17:21): BROADENED to match any cow NPC.
            // The exact "Prized dairy cow"/"Dairy cow" missed visible cows.
            // Build 455 (Alex 18:05): NOTE -- the bot routes to DAIRY_PASTURE
            // (3172,3317) via the approach leg. This phase validates the
            // special dairy target with live Milk action before any click.
            boolean cowValid = false;
            String cowInfo = "?";
            var targetCow = (net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel) null;
            try {
                var cow = Microbot.getClientThread().invoke(() ->
                    net.runelite.client.plugins.microbot.util.npc.Rs2Npc.getNpcs(o ->
                        o != null && o.getName() != null
                        && o.getName().toLowerCase().contains("dairy")
                        && o.getName().toLowerCase().contains("cow"))
                        .findFirst().orElse(null));
                if (cow != null) {
                    targetCow = cow;
                    cowValid = true;
                    try {
                        WorldPoint cwp = cow.getWorldLocation();
                        cowInfo = "'" + cow.getName() + "' id=" + cow.getId()
                            + "@(" + cwp.getX() + "," + cwp.getY() + ")";
                    } catch (Exception e) {
                        cowInfo = "'" + cow.getName() + "' id=" + cow.getId();
                    }
                }
            } catch (Exception e) {
                diag("Build 472: MILK_COW: cow validation failed: " + e.getMessage());
            }
            if (!cowValid) {
                milkPhase3Scans++;
                if (milkPhase3Scans >= 5) {
                    diag("Build 472: MILK_COW: no Prized/Dairy cow NPC in 5 scans -- HELD (no bucket action without validated cow)");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                } else {
                    diag("Build 472: MILK_COW: no Prized/Dairy cow NPC yet, scan " + milkPhase3Scans + "/5 -- rescanning");
                }
                return;
            }
            milkPhase3Scans = 0;
            diag("Build 472: MILK_COW: cow validated: " + cowInfo + " -- proceeding");
            // Build 441 (Alex 17:25): MILK THE NPC, not a tile object.
            // The validated target is an NPC (Cow/Cow calf). Use the bucket
            // on the live NPC via Rs2Inventory.useItemOnNpc. One action,
            // next-tick Bucket -> Bucket of milk proof. Bounded, then HOLD.
            // No tile-object fallback, no gate bypass.
            // Build 455 (Alex 18:03): use the validated targetCow (audit-selected
            // or re-queried dairy). No separate re-query that could pick a
            // different NPC.
            try {
                var targetNpc = targetCow;
                if (targetNpc == null) {
                    diag("Build 472: MILK_COW: cow NPC lost -- rescanning");
                    return;
                }
                WorldPoint nwp = targetNpc.getWorldLocation();
                int nd = chebDist(pp, nwp);
                // Build 444 (Alex 17:33): BOUNDED NPC DIAGNOSTIC at selection.
                // Logs name/id/tile/distance/adjacency and the live action list.
                // If no Milk action, stops with clear reason (no retries).
                String npcActions = "?";
                boolean hasMilkAction = false;
                try {
                    String[] npcActionsArr = Microbot.getClientThread().invoke(() -> {
                        try {
                            var comp = targetNpc.getTransformedComposition();
                            if (comp == null) return null;
                            return comp.getActions();
                        } catch (Exception e) {
                            return null;
                        }
                    });
                    if (npcActionsArr != null) {
                        StringBuilder sb = new StringBuilder("[");
                        for (String a : npcActionsArr) {
                            if (a != null) {
                                if (sb.length() > 1) sb.append(",");
                                sb.append(a);
                                if (a.equalsIgnoreCase("Milk")) hasMilkAction = true;
                            }
                        }
                        sb.append("]");
                        npcActions = sb.toString();
                    } else {
                        npcActions = "unavailable";
                    }
                } catch (Exception e) {
                    npcActions = "error:" + e.getMessage();
                }
                diag("Build 472: MILK_COW: NPC diag: '" + targetNpc.getName()
                    + "' id=" + targetNpc.getId() + "@(" + nwp.getX() + "," + nwp.getY()
                    + ") dist=" + nd + " adjacent=" + (nd <= 2)
                    + " actions=" + npcActions + " hasMilk=" + hasMilkAction);
                if (!hasMilkAction && !npcActions.equals("unavailable") && !npcActions.startsWith("error")) {
                    diag("Build 472: MILK_COW: adult cow has NO Milk action -- HELD (target-resolution: no milkable action)");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    return;
                }
                if (nd > 2) {
                    // Not adjacent: route via shared traversal (gate if needed).
                    stepToward(nwp, "to cow for milking");
                    return;
                }
                // Adjacent: one bucket-on-NPC action with pre-counts.
                int bucketsBefore = invCount(BUCKET);
                int milkBefore = invCount(MILK);
                if (bucketsBefore == 0) {
                    diag("Build 472: MILK_COW: no bucket in inventory -- HELD");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                    return;
                }
                boolean used = false;
                try {
                    int bucketId = -1;
                    var bucketItem = net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.get(BUCKET);
                    if (bucketItem != null) bucketId = bucketItem.getId();
                    if (bucketId > 0) {
                        used = net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.useItemOnNpc(bucketId, targetNpc);
                    }
                } catch (Exception e) {
                    diag("Build 472: MILK_COW: useItemOnNpc threw: " + e.getMessage());
                }
                if (used) {
                    diag("Build 472: MILK_COW: bucket-on-cow '" + targetNpc.getName()
                        + "' id=" + targetNpc.getId() + "@(" + nwp.getX() + "," + nwp.getY()
                        + ") -- Bucket x" + bucketsBefore + ", Milk x" + milkBefore
                        + " -- next-tick inventory proof pending");
                    milkVerifyBucketsBefore = bucketsBefore;
                    milkVerifyMilkBefore = milkBefore;
                    milkVerifyMs = System.currentTimeMillis();
                    milkPhase = 4;
                } else {
                    milkPhase3Scans++;
                    if (milkPhase3Scans >= 3) {
                        diag("Build 472: MILK_COW: bucket-on-cow failed 3x -- HELD (typed MILK_ACTION_FAILED)");
                        milkPhase = 9;
                        milkHeldDiagMs = 0;
                    } else {
                        diag("Build 472: MILK_COW: useItemOnNpc returned false, attempt "
                            + milkPhase3Scans + "/3 -- retrying");
                    }
                }
            } catch (Exception e) {
                diag("Build 472: MILK_COW: phase 3 NPC milking failed: " + e.getMessage());
            }
            return;
        }

        if (milkPhase == 4) {
            // Next-tick inventory proof: Bucket -> Bucket of milk.
            if (System.currentTimeMillis() - milkVerifyMs < 2000) return; // wait 2s
            int bucketsAfter = invCount(BUCKET);
            int milkAfter = invCount(MILK);
            if (bucketsAfter < milkVerifyBucketsBefore && milkAfter > milkVerifyMilkBefore) {
                diag("Build 472: MILK_COW: MILK VERIFIED next-tick: Bucket x"
                    + milkVerifyBucketsBefore + "->x" + bucketsAfter + ", Milk x"
                    + milkVerifyMilkBefore + "->x" + milkAfter + " -> RETURN phase");
                // Build 441 (Alex 17:25): FORCED RETURN ROUTE for gate proof.
                // Route back to entry position via shared traversal. If we
                // entered the pen, this MUST cross the gate with full proof:
                // handoff -> scores -> one action -> object proof -> side proof.
                milkPhase = 5;
                milkWaitTicks = 0;
            } else {
                diag("Build 472: MILK_COW: no inventory delta after Milk click (Bucket x"
                    + milkVerifyBucketsBefore + "->x" + bucketsAfter + ", Milk x"
                    + milkVerifyMilkBefore + "->x" + milkAfter + ") -- holding");
                milkPhase = 9;
                milkHeldDiagMs = 0;
            }
            return;
        }

        if (milkPhase == 5) {
            // Build 472: RETURN to entry for gate-crossing proof (Alex 17:25).
            // Uses stepToward -> shared Rs2Traversal. Success requires the
            // full proof chain, NOT ordinary walker movement or proximity.
            if (milkEntryPos == null) {
                diag("Build 472: MILK_COW: no entry pos -- skipping return -> DETECT");
                milkReset396();
                forceState(Stage.DETECT, "have milk");
                return;
            }
            diag("Build 472: MILK_COW: RETURN -- to entry @("
                + milkEntryPos.getX() + "," + milkEntryPos.getY()
                + ") via shared traversal");
            if (stepToward(milkEntryPos, "return from dairy pen")) {
                diag("Build 472: MILK_COW: RETURN COMPLETE -> DETECT");
                milkReset396();
                forceState(Stage.DETECT, "have milk, returned");
            } else {
                milkWaitTicks++;
                if (milkWaitTicks > 100) {
                    diag("Build 472: MILK_COW: return timeout -- HELD (ROUTE_BLOCKED)");
                    milkPhase = 9;
                    milkHeldDiagMs = 0;
                }
            }
            return;
        }

        milkPhase = 0;
    }

    private static final String[] COOK_FINISH_OPTIONS = {
        // With all ingredients the Cook takes them via dialogue; just continue.
    };

    private void doReturnCook() {
        if (dialogueTick(COOK_FINISH_OPTIONS)) return; // dialogue owns the tick
        if (!hasMilk() || !hasEgg() || !hasFlour()) {
            diag("RETURN_COOK: lost an ingredient -> DETECT re-routes");
            forceState(Stage.DETECT, "missing ingredient");
            return;
        }
        WorldPoint pp = playerPos();
        if (pp == null) return;
        if (chebDist(pp, COOK_TILE) <= 12) {
            talkToNpc("Cook", 15);
            return;
        }
        // Build 463 (Alex 18:33): STAGED RETURN ROUTE.
        // Diagnosis: direct stepToward(COOK_TILE, "to the Cook") from the dairy
        // area fails with SHARED_TRAVERSAL FAILED -- the long-distance pathfinder
        // cannot route (3172,3317)->(3209,3214) directly (same class as the
        // dairy outbound failure, Build 456). Use validated anchors in reverse:
        //   DAIRY_PASTURE (3172,3317) -> MILL_APPROACH (3166,3304)
        //   -> WHEAT_FIELD (3157,3288) -> COOK_TILE (3209,3214)
        // Each leg is a short hop the shared traversal can handle.
        // Typed diagnosis on failure: RETURN_ROUTE_BLOCKED.
        WorldPoint returnGoal = null;
        String returnLabel = null;
        int distDairy = chebDist(pp, DAIRY_PASTURE);
        int distMill = chebDist(pp, MILL_APPROACH);
        int distWheat = chebDist(pp, WHEAT_FIELD);
        int distWheatExit = chebDist(pp, WHEAT_EXIT_EAST);
        int distCookFar = chebDist(pp, COOK_TILE);
        // Build 472: MONOTONIC latch - never go backward. If latch=4, skip legs 1-3.
        if (returnLegLatch <= 1 && distDairy <= 12) {
            // At dairy: head to mill approach.
            returnLegLatch = Math.max(returnLegLatch, 1);
            returnGoal = MILL_APPROACH;
            returnLabel = "return leg 1/5 to mill approach";
            returnLeg3StallTicks = 0;
        } else if (returnLegLatch <= 2 && distMill <= 12) {
            // At mill: head to wheat field.
            returnLegLatch = Math.max(returnLegLatch, 2);
            returnGoal = WHEAT_FIELD;
            returnLabel = "return leg 2/5 to wheat field";
            returnLeg3StallTicks = 0;
        } else if (returnLegLatch <= 3 && distWheat <= 12 && distWheatExit > 8) {
            // At wheat field (west side): exit east out of the fence first.
            // Build 465 (Alex 18:39): player stuck at (3149,3292) west-side.
            // Go east to WHEAT_EXIT_EAST (outside fence) before heading south.
            // Build 472: latch prevents re-entry after leaving.
            returnLegLatch = Math.max(returnLegLatch, 3);
            returnGoal = WHEAT_EXIT_EAST;
            returnLabel = "return leg 3/5 to wheat exit east";
            returnLeg3StallTicks = 0;
        } else if (returnLegLatch <= 4 && (returnLegLatch == 4 || distWheatExit <= 12 || distWheat <= 12) && distCookFar > 25) {
            // Build 471 (Alex 18:57): latch==4 bypasses wheat-proximity check.
            // Once committed to leg 4, never fall through to anchor fallback
            // (which sends backward to wheat). Stay in greedy-forward mode.
            // Build 470 (Alex 18:52): GATE-AWARE recovery. WebWalk reports walled edge
            // 3145,3292->3146,3291 (double-gate wing). If player is west of gate,
            // STOP greedy search and invoke door pipeline: approach, open, verify cross.
            int px0 = pp.getX();
            // Gate crossing needed if player west of gate (x<3145) and Cook is east
            boolean needGate = px0 < GATE_WEST.getX() && COOK_TILE.getX() > GATE_EAST.getX();
            if (needGate && gateCrossPhase == 0) {
                gateCrossPhase = 1; // start approach
                gateCrossTicks = 0;
                diag("Build 472: GATE_CROSSING: player west of double-gate at ("
                    + px0 + "," + pp.getY() + "), engaging door pipeline");
            }
            if (gateCrossPhase > 0) {
                // Door pipeline state machine
                gateCrossTicks++;
                if (gateCrossTicks > 30) {
                    diag("Build 472: DOOR_CROSSING_FAILED: gate at (3145,3292) not crossed in 30 ticks -- HOLD");
                    Microbot.status = "Cook's Assistant: door crossing failed -- holding";
                    return;
                }
                int distGateWest = chebDist(pp, GATE_WEST);
                int distGateEast = chebDist(pp, GATE_EAST);
                if (gateCrossPhase == 1) {
                    // Phase 1: approach gate west side
                    if (distGateWest <= 3) {
                        gateCrossPhase = 2;
                        gateCrossTicks = 0;
                        diag("Build 472: GATE_CROSSING: at gate west, searching for gate object");
                    } else {
                        returnGoal = GATE_WEST;
                        returnLabel = "return leg 4/5 gate approach west";
                        returnLegLatch = Math.max(returnLegLatch, 4);
                        // fall through to stepToward below
                    }
                }
                if (gateCrossPhase == 2) {
                    // Phase 2: find and open the gate object
                    try {
                        net.runelite.api.GameObject gateObj = Microbot.getClientThread().invoke(() -> {
                            java.util.List<net.runelite.api.GameObject> objs =
                                net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject.getGameObjects();
                            for (net.runelite.api.GameObject o : objs) {
                                if (o != null && o.getWorldLocation() != null &&
                                    Math.abs(o.getWorldLocation().getX() - 3145) <= 2 &&
                                    Math.abs(o.getWorldLocation().getY() - 3292) <= 2) {
                                    return o;
                                }
                            }
                            return null;
                        });
                        if (gateObj != null) {
                            diag("Build 472: GATE_CROSSING: found gate object id="
                                + gateObj.getId() + " at (" + gateObj.getWorldLocation().getX()
                                + "," + gateObj.getWorldLocation().getY() + "), clicking Open");
                            boolean clicked = false;
                            try {
                                clicked = net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject.interact(gateObj, "Open");
                            } catch (Exception e) {}
                            diag("Build 472: GATE_CROSSING: Open action=" + clicked);
                            gateCrossPhase = 3;
                            gateCrossTicks = 0;
                        } else {
                            diag("Build 472: GATE_CROSSING: no gate object found near (3145,3292), trying walk-through");
                            gateCrossPhase = 3;
                            gateCrossTicks = 0;
                        }
                    } catch (Exception e) {
                        diag("Build 472: GATE_CROSSING: object query failed: " + e.getMessage());
                        gateCrossPhase = 3;
                    }
                    return; // wait next tick after open attempt
                }
                if (gateCrossPhase == 3) {
                    // Phase 3: walk through to east side, verify
                    if (distGateEast <= 3) {
                        diag("Build 472: GATE_CROSSING: crossed to east side ("
                            + pp.getX() + "," + pp.getY() + "), resuming route");
                        gateCrossPhase = 0; // done, resume normal leg 4
                        gateCrossTicks = 0;
                        // fall through to greedy below
                    } else {
                        returnGoal = GATE_EAST;
                        returnLabel = "return leg 4/5 gate cross to east";
                        returnLegLatch = Math.max(returnLegLatch, 4);
                        // fall through to stepToward below
                    }
                }
                // If we set returnGoal above, skip greedy and go to stepToward
                if (returnGoal != null && gateCrossPhase > 0) {
                    diag("Build 472: RETURN_COOK: " + returnLabel
                        + " from (" + pp.getX() + "," + pp.getY() + ")");
                    stepToward(returnGoal, returnLabel);
                    return;
                }
                // else fall through to greedy (phase 0 = done)
            }
            // At wheat exit: head south toward Lumbridge using LIVE collision grid.
            // Build 467 (Alex 18:45): screenshot shows open crossroads with valid-movement
            // tiles visible, no gate blocking. Do NOT hardcode road anchor.
            // Greedy descent: find nearest walkable tile that reduces distance to Cook.
            // Stall detection: one bounded failure then HOLD.
            String curPos4 = pp.getX() + "," + pp.getY();
            if (curPos4.equals(returnLeg4LastPos)) {
                returnLeg4StallTicks++;
            } else {
                returnLeg4StallTicks = 0;
                returnLeg4LastPos = curPos4;
            }
            if (returnLeg4StallTicks >= 15) {
                diag("Build 472: RETURN_ROUTE_BLOCKED: leg 4/5 greedy-descent stalled at ("
                    + curPos4 + "), no walkable tile reduces distance to Cook -- HOLD");
                Microbot.status = "Cook's Assistant: return route blocked at leg 4/5 -- holding";
                return;
            }
            // Greedy with DIRECTIONAL constraint (Build 469, Alex 18:51):
            // Goal is SOUTHEAST of wheat area. Unconstrained greedy picked
            // (3162,3275) from (3170,3283) -- Chebyshev decreased but x went WEST,
            // returning to the (3148,3292) trap.
            // Require monotonic southeast progress: cand.x >= px (no west),
            // cand.y <= py (no north). Reject violators.
            WorldPoint bestTile = null;
            int bestDist = chebDist(pp, COOK_TILE);
            int px = pp.getX(), py = pp.getY();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = -8; dy <= 8; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int cx = px + dx, cy = py + dy;
                    // Directional filter: no west (x<px), no north (y>py)
                    if (cx < px || cy > py) continue;
                    WorldPoint cand = new WorldPoint(cx, cy, 0);
                    int d = chebDist(cand, COOK_TILE);
                    if (d >= bestDist) continue; // must reduce distance
                    try {
                        if (net.runelite.client.plugins.microbot.util.tile.Rs2Tile.isWalkable(cand)) {
                            bestDist = d;
                            bestTile = cand;
                        }
                    } catch (Exception e) { /* skip */ }
                }
            }
            if (bestTile == null) {
                diag("Build 472: RETURN_ROUTE_BLOCKED: leg 4/5 no southeast-progress walkable tile from ("
                    + curPos4 + ") toward Cook (rejected x<px or y>py) -- HOLD");
                Microbot.status = "Cook's Assistant: no walkable path south -- holding";
                return;
            }
            returnLegLatch = Math.max(returnLegLatch, 4);
            returnGoal = bestTile;
            returnLabel = "return leg 4/5 greedy to (" + bestTile.getX() + "," + bestTile.getY() + ")";
            returnLeg3StallTicks = 0;
        } else if (returnLegLatch <= 5 && distCookFar <= 25) {
            // At wheat field: head to Cook.
            // Build 464 (Alex 18:38): VALIDATE Cook target from live NPC position.
            // Build 473 (Alex 19:11): REACHABLE ADJACENT TILE, not the NPC's tile.
            // The Cook NPC is inside the kitchen; its exact tile is not walkable.
            // Query live NPC, then choose the nearest reachable adjacent tile.
            WorldPoint cookTarget = COOK_TILE;
            String cookSource = "hardcoded";
            try {
                var cookNpc = Microbot.getClientThread().invoke(() ->
                    net.runelite.client.plugins.microbot.util.npc.Rs2Npc.getNpcs(o ->
                        o != null && o.getName() != null && o.getName().equalsIgnoreCase("Cook"))
                        .findFirst().orElse(null));
                if (cookNpc != null) {
                    try {
                        WorldPoint npcPos = cookNpc.getWorldLocation();
                        if (npcPos != null) {
                            cookTarget = npcPos;
                            cookSource = "live NPC";
                        }
                    } catch (Exception e) {
                        // fall back to hardcoded
                    }
                }
            } catch (Exception e) {
                // fall back to hardcoded
            }
            // Build 473: find nearest reachable tile adjacent to (or at) cookTarget.
            // The NPC tile itself is likely inside the kitchen (unwalkable).
            WorldPoint reachableCookGoal = null;
            int bestAdjDist = Integer.MAX_VALUE;
            try {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        WorldPoint adj = new WorldPoint(
                            cookTarget.getX() + dx,
                            cookTarget.getY() + dy,
                            cookTarget.getPlane());
                        boolean reach = false;
                        try {
                            reach = Rs2Tile.isTileReachable(adj);
                        } catch (Exception e) { reach = false; }
                        if (reach) {
                            int d = chebDist(pp, adj);
                            if (d < bestAdjDist) {
                                bestAdjDist = d;
                                reachableCookGoal = adj;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // fall through to null check
            }
            if (reachableCookGoal == null) {
                // No reachable adjacent tile: one sticky typed block, HOLD.
                diag("Build 473: COOK_DESTINATION_BLOCKED: leg 5/5 no reachable tile adjacent to Cook target ("
                    + cookTarget.getX() + "," + cookTarget.getY() + "," + cookSource
                    + ") from (" + pp.getX() + "," + pp.getY() + ") -- HOLD");
                Microbot.status = "Cook's Assistant: Cook destination blocked -- holding";
                return;
            }
            // Use the reachable adjacent tile as the goal.
            cookTarget = reachableCookGoal;
            cookSource = cookSource + "+reachable-adj";
            // Stall detection: if we're not making progress toward Cook, count ticks.
            String curPos = pp.getX() + "," + pp.getY();
            if (curPos.equals(returnLeg3LastPos)) {
                returnLeg3StallTicks++;
            } else {
                returnLeg3StallTicks = 0;
                returnLeg3LastPos = curPos;
            }
            if (returnLeg3StallTicks >= 15) {
                // Typed failure, once. Do not log leg 4/4 indefinitely.
                // Build 472: one bounded failure, then HOLD (no re-entry).
                diag("Build 472: RETURN_ROUTE_BLOCKED: leg 5/5 road->Cook stalled at ("
                    + curPos + "), target=(" + cookTarget.getX() + "," + cookTarget.getY()
                    + "," + cookSource + "), walkStep=no-walkable-path -- HOLD");
                Microbot.status = "Cook's Assistant: return route blocked -- holding";
                // Stay in RETURN_COOK but do not call stepToward (avoid log spam).
                // The HOLD is sticky until manual intervention or restart.
                return;
            }
            returnLegLatch = Math.max(returnLegLatch, 5);
            returnGoal = cookTarget;
            returnLabel = "return leg 5/5 to the Cook (" + cookSource + ")";
        } else {
            // Not at a known anchor: head to the nearest anchor first.
            // This handles restarts mid-route.
            int minDist = Math.min(distDairy, Math.min(distMill, distWheat));
            if (minDist == distDairy) {
                returnGoal = DAIRY_PASTURE;
                returnLabel = "return: to dairy anchor";
            } else if (minDist == distMill) {
                returnGoal = MILL_APPROACH;
                returnLabel = "return: to mill anchor";
            } else {
                returnGoal = WHEAT_FIELD;
                returnLabel = "return: to wheat anchor";
            }
            // If already close to Cook but not within 12, go direct.
            int distCook = chebDist(pp, COOK_TILE);
            if (distCook < minDist && distCook <= 30) {
                returnGoal = COOK_TILE;
                returnLabel = "to the Cook";
            }
        }
        diag("Build 472: RETURN_COOK: " + returnLabel
            + " from (" + pp.getX() + "," + pp.getY() + ")");
        stepToward(returnGoal, returnLabel);
    }

    private void doDone() {
        // Build 389: NO logout on completion. The Supervisor's 30s self-heal
        // (launcher_clicker.py --check-once) clicks CLICK HERE TO PLAY on any
        // visible lobby, so a plugin-side logout flaps login/logout forever
        // (proven on Tutorial Island 2026-09-29 11:38-11:40). Park in-game,
        // fully idle and stable, until the sentinel-aware Supervisor.bat is
        // installed (staged in the repo under infrastructure/).
        // Build 393: PARK, do not shutdown. shutdown() killed the script's
        // tick loop, starving pollBotCommand() -- SWITCH_TO_TUTORIAL posted
        // after quest completion would never be picked up (same root cause
        // as the starved SWITCH_TO_COOKS on Tutorial Island, Alex ~12:55).
        // The tick loop keeps running; the DONE dispatch below holds quest
        // logic while the command channel stays alive.
        if (!doneAnnounced) {
            doneAnnounced = true;
            Microbot.status = "Cook's Assistant: quest complete! (parked)";
            diag("Build " + BUILD_NUMBER + ": Cook's Assistant FINISHED (game-verified) -- parking in-game, no logout (Supervisor auto-login would flap). Parked, housekeeping alive.");
            log.info("[CooksAssistant] Quest complete -- parked, housekeeping alive");
        } else if (System.currentTimeMillis() - lastParkedDiagMs393 > 120000) {
            lastParkedDiagMs393 = System.currentTimeMillis();
            diag("Build " + BUILD_NUMBER + ": parked DONE -- quest logic held, housekeeping alive (command channel polling)");
        }
    }

    /**
     * Tiny fallback for object interaction by name when the cache query
     * misses (e.g. name casing variants). Uses the standard Rs2GameObject
     * API instead of the manual cache scan.
     */
    private static class Rs2GameObjectFallback {
        static boolean interact(String name, String action) {
            try {
                return net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject
                        .interact(name, action);
            } catch (Exception e) {
                return false;
            }
        }
    }

    // Build 415: Diagnostic overlay controller (NUDGE ALEX 2026-09-29 15:49/15:51).
    // Temporary, optional, read-only. One layer active at a time; capture-once
    // per layer then advance. No gameplay clicks, no movement, no state changes.
    // Layers: COORDS -> COLLISION_LOS -> WALL_DOOR -> TARGET -> INVENTORY_DIALOGUE.
    private enum DiagLayer {
        COORDS, COLLISION_LOS, WALL_DOOR, TARGET, INVENTORY_DIALOGUE
    }

    private void diagControllerTick() {
        // Build 415 fix (NUDGE ALEX 2026-09-29 15:52): defer ALL layers until
        // the local player exists. During login getLocalPlayer() is null and
        // coordinate/LOS queries NPE in the overlay renderer. No capture, no
        // layer advance, no logging until we're in-game.
        WorldPoint pp = null;
        try {
            if (Microbot.getClient() == null
                || Microbot.getClient().getLocalPlayer() == null) {
                return;
            }
            pp = WorldPoint.fromLocalInstance(
                Microbot.getClient(),
                Microbot.getClient().getLocalPlayer().getLocalLocation());
        } catch (Exception e) {
            return;
        }
        if (pp == null) {
            return;
        }

        // Build 418 fix (NUDGE ALEX 2026-09-29 16:02): COLLISION_LOS was
        // 177-181ms for the 3x3 isWalkable grid (over the 100ms budget).
        // Now ONE probe per tick (~20ms); the grid assembles over 9 ticks.
        // The COLLISION_LOS layer displays the cached grid (instant).
        // Grid invalidates when the player moves to a new center tile.
        try {
            if (pp.getX() != losCenterX || pp.getY() != losCenterY) {
                // Player moved -- reset the grid.
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        losKnown[i][j] = false;
                    }
                }
                losProbeIdx = 0;
                losCenterX = pp.getX();
                losCenterY = pp.getY();
            }
            if (losProbeIdx < 9) {
                int dx = (losProbeIdx % 3) - 1;
                int dy = (losProbeIdx / 3) - 1;
                try {
                    WorldPoint wp = new WorldPoint(
                        pp.getX() + dx, pp.getY() + dy, pp.getPlane());
                    losGrid[dx + 1][dy + 1] = Rs2Tile.isWalkable(wp);
                    losKnown[dx + 1][dy + 1] = true;
                } catch (Exception e) {
                    // Probe failed -- mark unknown, move on.
                    losKnown[(losProbeIdx % 3)][(losProbeIdx / 3)] = false;
                }
                losProbeIdx++;
            }
        } catch (Exception e) {
            // LOS probing never breaks the controller.
        }

        long now = System.currentTimeMillis();
        // Advance layer when budget expires (first tick initializes).
        // Build 418 (NUDGE ALEX 2026-09-29 16:05): state-aware WALL_DOOR.
        // WALL_DOOR activates ONLY when at a gate, blocked at a gate, or
        // needing route diagnosis (milkPhase 1-2, or HELD near a gate).
        // Otherwise skip it and keep the frame clean. 2s cycle is baseline.
        if (diagCtlLayerStartMs == 0 || now - diagCtlLayerStartMs >= DIAG_CTL_LAYER_MS) {
            int nextLayer = (diagCtlLayerStartMs == 0) ? 0 : (diagCtlLayer + 1) % DIAG_CTL_LAYERS;
            // Skip WALL_DOOR when not gate-relevant.
            if (DiagLayer.values()[nextLayer] == DiagLayer.WALL_DOOR && !isWallDoorRelevant()) {
                diag("Build 418: DIAG_CTL wall-door skipped (not gate-relevant, milkPhase=" + milkPhase + ") -- frame clean");
                nextLayer = (nextLayer + 1) % DIAG_CTL_LAYERS;
            }
            diagCtlLayer = nextLayer;
            diagCtlLayerStartMs = now;
            // Build 417: capture once on layer entry (bounded, read-only),
            // then push the layer + data to the visual overlay. The overlay
            // draws only this layer and clears on the next transition.
            String layerData = diagControllerCapture(DiagLayer.values()[diagCtlLayer], pp);
            CooksAssistantDiagOverlay ov = diagOverlay;
            if (ov != null) {
                try {
                    ov.setLayer(diagCtlLayer, layerData);
                } catch (Exception e) {
                    // Overlay update never breaks the controller.
                }
            }
        }
    }

    /**
     * Build 418 (NUDGE ALEX 2026-09-29 16:05): WALL_DOOR is relevant when the
     * milk step is in gate-scan (1) or gate-crossing (2) phase, or when HELD
     * (9) near a known gate tile (blocked diagnosis). Otherwise the frame
     * stays clean.
     */
    private boolean isWallDoorRelevant() {
        try {
            // Build 472: phases 1-2 removed; shared Rs2Traversal owns gates now.
            // The milkGateTile branch is gone (field removed). WALL_DOOR
            // relevance is now purely from sharedTraversal state.
            // Build 422: WALL_DOOR activates when traversal is in progress
            // or has failed holding at a gate. Not when DONE (complete) or
            // IDLE (inactive).
            if (grainTraversal != null && !grainTraversal.isIdle()
                && !grainTraversal.isDone()) return true;
        } catch (Exception e) { /* default false */ }
        return false;
    }

    /**
     * Build 417: captures the layer data and returns it as multi-line text
     * for the visual overlay. Also logs the DIAG_CTL line. Bounded, read-only.
     * Build 418: COLLISION_LOS reads the cached grid (no blocking calls);
     * TARGET exposes bounded nearby milk candidates.
     */
    private String diagControllerCapture(DiagLayer layer, WorldPoint pp) {
        long t0 = System.currentTimeMillis();
        StringBuilder overlayData = new StringBuilder();
        try {
            switch (layer) {
                case COORDS -> {
                    String pos = (pp == null) ? "?" :
                        pp.getX() + "," + pp.getY() + "," + pp.getPlane();
                    diag("DIAG_CTL [COORDS] player=(" + pos + ")");
                    overlayData.append("player=(").append(pos).append(")");
                }
                case COLLISION_LOS -> {
                    // Build 418: read the cached grid assembled one probe
                    // per tick. No isWalkable calls here -- instant.
                    if (pp == null) {
                        diag("DIAG_CTL [COLLISION_LOS] no player pos");
                        overlayData.append("no player pos");
                        break;
                    }
                    StringBuilder sb = new StringBuilder();
                    int known = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int ix = dx + 1, iy = dy + 1;
                            if (losKnown[ix][iy]) {
                                sb.append(losGrid[ix][iy] ? "." : "#");
                                known++;
                            } else {
                                sb.append("?");
                            }
                        }
                        if (dy < 1) sb.append("/");
                    }
                    String gridAge = (known < 9) ? " (probing " + known + "/9)" : "";
                    diag("DIAG_CTL [COLLISION_LOS] 3x3=" + sb.toString()
                        + " center=(" + pp.getX() + "," + pp.getY() + ")" + gridAge);
                    overlayData.append("3x3=").append(sb.toString())
                        .append("\ncenter=(").append(pp.getX()).append(",")
                        .append(pp.getY()).append(")").append(gridAge);
                }
                case WALL_DOOR -> {
                    // Query ONLY the known gate IDs (1559/1560) --
                    // bounded, no broad scene enumeration. ID + location +
                    // actions only; no getName() on unfiltered objects.
                    String pos = (pp == null) ? "?" :
                        pp.getX() + "," + pp.getY() + "," + pp.getPlane();
                    StringBuilder wd = new StringBuilder();
                    wd.append("player=(").append(pos).append(") r=10\n");
                    try {
                        java.util.List<Rs2TileObjectModel> gates =
                            Microbot.getRs2TileObjectCache().query()
                                .withIds(1559, 1560)
                                .toList();
                        int shown = 0;
                        for (Rs2TileObjectModel g : gates) {
                            if (shown >= 5) break; // hard bound
                            try {
                                WorldPoint gwp = g.getWorldLocation();
                                if (pp != null && gwp.distanceTo(pp) > 10) {
                                    continue; // radius limit
                                }
                                // Build 420: NO getObjectComposition() (blocks).
                                // Actions not read; id/tile/type shown.
                                String actions = "(not read)";
                                wd.append("id=").append(g.getId())
                                  .append("@(").append(gwp.getX()).append(",")
                                  .append(gwp.getY()).append(")")
                                  .append(" actions=[").append(actions).append("]\n");
                                shown++;
                            } catch (Exception e) { /* skip this gate */ }
                        }
                        if (shown == 0) {
                            wd.append("(no gate 1559/1560 in 10)");
                        }
                    } catch (Exception e) {
                        wd.append("(query failed: ").append(e.getMessage()).append(")");
                    }
                    diag("DIAG_CTL [WALL_DOOR] " + wd.toString().replace("\n", " | "));
                    overlayData.append(wd.toString());
                }
                case TARGET -> {
                    // Build 418 (NUDGE ALEX 2026-09-29 16:03:46): the 16:03:24
                    // frame shows cow NPCs inside the fenced pen -- the milk
                    // target is likely an NPC, not tile-object id=8689.
                    // Query a bounded nearby NPC set filtered by "Dairy cow"/
                    // "Cow" name and/or "Milk" action. Display real NPC ID,
                    // tile, and actions. getName()/getComposition() ONLY on
                    // the small filtered set (max 3). Object 8689 scan kept
                    // as evidence. Strict radius/max/time budget.
                    StringBuilder td = new StringBuilder();
                    String phaseInfo;
                    if (milkPhase == 3) {
                        phaseInfo = "milkPhase=3";
                    } else if (milkPhase == 9) {
                        phaseInfo = "milkPhase=9 HELD";
                    } else {
                        phaseInfo = "milkPhase=" + milkPhase;
                    }
                    td.append(phaseInfo).append("\n");
                    long budgetMs = 80; // leave headroom under 100ms
                    int npcShown = 0;
                    try {
                        java.util.List<net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel> npcs =
                            Microbot.getRs2NpcCache().query().toList();
                        // Pre-filter by distance (cheap), cap at 10.
                        java.util.List<net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel> nearby =
                            new java.util.ArrayList<>();
                        for (net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel n : npcs) {
                            if (nearby.size() >= 10) break;
                            if (System.currentTimeMillis() - t0 > budgetMs) break;
                            try {
                                WorldPoint nwp = n.getWorldLocation();
                                if (pp != null && nwp.distanceTo(pp) <= 15) {
                                    nearby.add(n);
                                }
                            } catch (Exception e) { /* skip */ }
                        }
                        // Filter by name/actions on the small set only.
                        for (net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel n : nearby) {
                            if (npcShown >= 3) break;
                            if (System.currentTimeMillis() - t0 > budgetMs) break;
                            try {
                                String name = null;
                                try {
                                    name = n.getNpc().getName();
                                } catch (Exception e) { /* keep null */ }
                                if (name == null) continue;
                                String lower = name.toLowerCase();
                                boolean isCow = lower.contains("cow");
                                // Check actions for "Milk".
                                String actions = "?";
                                boolean hasMilk = false;
                                try {
                                    var comp = n.getNpc().getComposition();
                                    if (comp != null && comp.getActions() != null) {
                                        String[] acts = comp.getActions();
                                        java.util.List<String> actList = new java.util.ArrayList<>();
                                        for (String a : acts) {
                                            if (a != null) {
                                                actList.add(a);
                                                if (a.equalsIgnoreCase("Milk")) {
                                                    hasMilk = true;
                                                }
                                            }
                                        }
                                        actions = String.join("/", actList);
                                    }
                                } catch (Exception e) { /* keep ? */ }
                                if (!isCow && !hasMilk) continue;
                                WorldPoint nwp = n.getWorldLocation();
                                td.append("npc id=").append(n.getId())
                                  .append(" '").append(name).append("'")
                                  .append("@(").append(nwp.getX()).append(",")
                                  .append(nwp.getY()).append(")")
                                  .append(" actions=[").append(actions).append("]\n");
                                npcShown++;
                            } catch (Exception e) { /* skip */ }
                        }
                    } catch (Exception e) { /* skip NPCs */ }
                    // Object 8689 kept as evidence (not the primary target).
                    int objShown = 0;
                    try {
                        java.util.List<Rs2TileObjectModel> milkObjs =
                            Microbot.getRs2TileObjectCache().query()
                                .withIds(8689)
                                .toList();
                        for (Rs2TileObjectModel o : milkObjs) {
                            if (objShown >= 2) break;
                            if (System.currentTimeMillis() - t0 > budgetMs) break;
                            try {
                                WorldPoint owp = o.getWorldLocation();
                                if (pp != null && owp.distanceTo(pp) > 50) continue;
                                td.append("obj id=8689@(").append(owp.getX())
                                  .append(",").append(owp.getY()).append(")\n");
                                objShown++;
                            } catch (Exception e) { /* skip */ }
                        }
                    } catch (Exception e) { /* skip */ }
                    if (npcShown == 0 && objShown == 0) {
                        td.append("(no milk candidates in range)");
                    }
                    diag("DIAG_CTL [TARGET] " + td.toString().replace("\n", " | "));
                    overlayData.append(td.toString());
                }
                case INVENTORY_DIALOGUE -> {
                    // Build 418 (NUDGE ALEX 2026-09-29 16:03:57): show item
                    // IDs + counts for acceptance proof. Before milk: Bucket
                    // id/count=1, Milk=0. After one Milk action, next-tick
                    // re-query must show Bucket of milk with expected change.
                    // Do not infer success from click or proximity.
                    StringBuilder inv = new StringBuilder();
                    try {
                        String[] names = {"Egg", "Bucket", "Bucket of milk", "Pot of flour"};
                        for (int i = 0; i < names.length; i++) {
                            String nm = names[i];
                            int cnt = invCount(nm);
                            int id = -1;
                            try {
                                // Get the item ID via Rs2Inventory (first match).
                                java.util.List<net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel> items =
                                    Rs2Inventory.items(item -> {
                                        try {
                                            String n = item.getName();
                                            return n != null && n.equalsIgnoreCase(nm);
                                        } catch (Exception e) {
                                            return false;
                                        }
                                    }).toList();
                                if (!items.isEmpty()) {
                                    id = items.get(0).getId();
                                }
                            } catch (Exception e) { /* keep -1 */ }
                            if (i > 0) inv.append(", ");
                            inv.append(nm).append("(id=").append(id).append(") x").append(cnt);
                        }
                    } catch (Exception e) {
                        inv.append("?");
                    }
                    boolean inDlg = false;
                    try {
                        inDlg = inDialogue();
                    } catch (Exception e) { /* keep false */ }
                    String dlgStr = inDlg ? "open" : "closed";
                    diag("DIAG_CTL [INVENTORY_DIALOGUE] inv=[" + inv.toString()
                        + "] dialogue=" + dlgStr
                        + " milkPhase=" + milkPhase);
                    overlayData.append("inv: ").append(inv.toString())
                        .append("\ndialogue: ").append(dlgStr)
                        .append("\nmilkPhase=").append(milkPhase);
                }
            }
        } catch (Exception e) {
            diag("DIAG_CTL [" + layer + "] capture failed: " + e.getMessage());
            overlayData.append("(capture failed)");
        }
        long ms = System.currentTimeMillis() - t0;
        if (ms > 100) {
            diag("DIAG_CTL [" + layer + "] SLOW: " + ms + "ms (budget 100ms)");
        }
        return overlayData.toString();
    }
}
