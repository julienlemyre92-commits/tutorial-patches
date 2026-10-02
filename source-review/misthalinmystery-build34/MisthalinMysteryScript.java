package net.runelite.client.plugins.microbot.misthalinmystery;

import java.io.InputStream;
import java.io.OutputStream;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.MessageNode;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.VarbitComposition;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.death.Rs2Death;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Misthalin Mystery route. All stage transitions are read from the live client. */
public final class MisthalinMysteryScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MisthalinMysteryScript.class);
    public static final int BUILD_NUMBER = 34;
    private static final int[] FOOD = {333, 329, 2309, 2140, 315};
    private static final int[] PUZZLE_BITS = {
        VarbitID.MISTMYST_CANDLE1, VarbitID.MISTMYST_CANDLE2,
        VarbitID.MISTMYST_CANDLE3, VarbitID.MISTMYST_CANDLE4,
        VarbitID.MISTMYST_PIANO_D1, VarbitID.MISTMYST_PIANO_E,
        VarbitID.MISTMYST_PIANO_A, VarbitID.MISTMYST_PIANO_D2,
        VarbitID.MISTMYST_PIANO_ATTEMPTS, VarbitID.MISTMYST_PIANO_DEAD,
        VarbitID.MISTMYST_SWITCH_ATTEMPTS, VarbitID.MISTMYST_SAPPHIRE_SWITCHED,
        VarbitID.MISTMYST_DIAMOND_SWITCHED, VarbitID.MISTMYST_ZENYTE_SWITCHED,
        VarbitID.MISTMYST_EMERALD_SWITCHED, VarbitID.MISTMYST_ONYX_SWITCHED,
        VarbitID.MISTMYST_RUBY_SWITCHED, VarbitID.MISTMYST_GEMS_SWITCHED
    };
    private static final Set<Integer> QUEST_ITEMS = Set.of(
        ItemID.MISTMYST_FRONTDOOR_KEY, ItemID.MISTMYST_RUBY_KEY,
        ItemID.MISTMYST_EMERALD_KEY, ItemID.MISTMYST_SAPPHIRE_KEY,
        ItemID.MISTMYST_CLUE_LIBRARY, ItemID.MISTMYST_CLUE_OUTSIDE,
        ItemID.MISTMYST_CLUE_KITCHEN, ItemID.MISTMYST_CUTSCENE_KNIFE,
        ItemID.KNIFE, ItemID.TINDERBOX, ItemID.BUCKET_EMPTY);
    private static final WorldPoint ABIGALE = p(3237, 3155), BOAT = p(3240, 3140),
        BUCKET = p(1619, 4816), BARREL = p(1615, 4829),
        FRONT_DOOR = p(1636, 4824), TABLE_KNIFE = p(1639, 4831),
        PINK_DOOR = p(1635, 4838), NOTE1 = p(1635, 4839),
        PAINTING = p(1632, 4833), RUBY_DOOR = p(1640, 4828),
        SHELVES = p(1646, 4826), CANDLE1 = p(1641, 4826),
        CANDLE2 = p(1647, 4827), CANDLE3 = p(1641, 4831),
        CANDLE4 = p(1646, 4832), EXPLOSIVE_BARREL = p(1647, 4830),
        WALL = p(1648, 4829), TREE = p(1630, 4849),
        NOTE2 = p(1632, 4850), PIANO = p(1647, 4841),
        EMERALD_DOOR = p(1633, 4837), DIAMOND_DOOR = p(1629, 4842),
        NOTE3 = p(1630, 4842), FIREPLACE = p(1647, 4836),
        SAPPHIRE_DOOR = p(1628, 4829), KILLER = p(1623, 4829),
        MANDY = p(1636, 4817);
    private static final Path STATUS = Paths.get(System.getProperty("user.home"),
        ".runelite", "misthalinmystery", "status.properties");
    private static final Path COMPLETE_MARKER = STATUS.resolveSibling("completed.flag");
    private static final Path CONTROL = STATUS.resolveSibling("control.properties");
    private static WorldPoint p(int x, int y) { return new WorldPoint(x, y, 0); }

    private enum Proof { STAGE, ITEM_PLUS, ITEM_MINUS, BIT_PLUS, BIT_CHANGE,
        DIALOGUE, WIDGET_OPEN, WIDGET_CLOSE, POS_CHANGE, ISLAND, OUTSIDE,
        BOSS, MIRROR_MOVE, EQUIPPED, BANK_OPEN, BANK_CLOSE, OBJECT_CHANGE, INSIDE,
        PROMO_CLOSED, BOSS_EXIT, MAINLAND, LOGGED_OUT,
        QUEST_FINISHED, HP_UP }
    private static final class Pending {
        final String key;
        final Proof proof;
        final Frame before;
        final int item, bit, object;
        final WorldPoint target;
        final long at, timeout;
        Pending(String key, Proof proof, Frame before, int item, int bit,
                int object, WorldPoint target, long timeout) {
            this.key=key; this.proof=proof; this.before=before; this.item=item;
            this.bit=bit; this.object=object; this.target=target;
            this.at=System.currentTimeMillis(); this.timeout=timeout;
        }
    }
    private static final class Graphic {
        final int id; final WorldPoint point;
        Graphic(int id, WorldPoint point) { this.id=id; this.point=point; }
        @Override public String toString() { return id+"@"+point; }
    }
    private static final class BarrelMenu {
        final NewMenuEntry entry;
        final Rectangle clickRegion;
        BarrelMenu(NewMenuEntry entry,Rectangle clickRegion) {
            this.entry=entry; this.clickRegion=clickRegion;
        }
    }
    private static final class Frame {
        GameState game; QuestState quest; WorldPoint pos, rawPos, templatePos;
        boolean instanced;
        String bucketInstances="";
        int varp=-1, rawVarp=-1, hp=-1, maxHp=-1, world, loginIndex, canvasWidth;
        boolean inCombat, bank, bankContentsAvailable, inventoryLoaded,
            inDialogue, hasContinue, pianoWidget, gemWidget, promoVisible,
            killerKnifeEquipped, paintingWestOpen;
        String dialogue="", dialogueWidgets="", selectedWidgetMeta="", recentGameMessage="",paintingType="",paintingCollision="",shelfProbe="",doorProbe="",candleVarbitLayout="";
        final List<String> options=new ArrayList<>();
        final Map<Integer,Integer> items=new HashMap<>(), bankItems=new HashMap<>(),
            bits=new HashMap<>();
        final Map<Integer,List<WorldPoint>> objects=new HashMap<>(),
            npcs=new HashMap<>();
        final List<Graphic> graphics=new ArrayList<>();
        int count(int id) { return items.getOrDefault(id,0); }
        int bankCount(int id) { return bankItems.getOrDefault(id,0); }
        int bit(int id) { return bits.getOrDefault(id,0); }
        WorldPoint npc(int id) {
            List<WorldPoint> list=npcs.get(id); return list==null||list.isEmpty()?null:list.get(0);
        }
        boolean objectAt(int id, WorldPoint tile) {
            List<WorldPoint> list=objects.get(id);
            return list!=null&&list.stream().anyMatch(tile::equals);
        }
        boolean island() { return pos!=null&&pos.getX()>=1600&&pos.getX()<=1679
            &&pos.getY()>=4800&&pos.getY()<=4852; }
        boolean outside() { return pos!=null
            && ((pos.getX()>=1648&&pos.getX()<=1654&&pos.getY()>=4825&&pos.getY()<=4852)
            || (pos.getX()>=1634&&pos.getX()<=1648&&pos.getY()>=4840&&pos.getY()<=4852)
            || (pos.getX()>=1631&&pos.getX()<=1633&&pos.getY()>=4847&&pos.getY()<=4850)); }
        boolean boss() { return pos!=null&&pos.getX()>=1619&&pos.getX()<=1627
            &&pos.getY()>=4825&&pos.getY()<=4834; }
        String dialogueSignature() { return inDialogue+":"+hasContinue+":"+dialogue+":"+options+":"+dialogueWidgets; }
    }
    private static final class Route {
        final String key; final WorldPoint target; final int radius;
        final long at=System.currentTimeMillis();
        volatile long endedAt;
        volatile boolean done, cancelled;
        volatile Thread worker;
        WorldPoint lastPos;
        long lastProgress=at;
        int bestDistance;
        int segments;
        Route(String key, WorldPoint target, int radius, WorldPoint pos) {
            this.key=key; this.target=target; this.radius=radius;
            this.lastPos=pos; this.bestDistance=distance(pos,target);
        }
    }
    private BooleanSupplier ownsInput;
    private MisthalinMysteryConfig config;
    private volatile boolean stopped, held, finished, bankPrepared, logoutSent;
    private String phase="START", error="", mirrorSignal="";
    private Pending pending;
    private Route route;
    private final Map<String,Integer> failures=new HashMap<>();
    private long nextActionAt, loginAt, welcomeAt, disconnectAt, stageAt;
    private int loginAttempts, welcomeAttempts, disconnectAttempts, selectedWorld;
    private int lastVarp=-1, lastHp=-1;
    private long lastStatusAt;
    private ScheduledFuture<?> telemetryFuture;
    private boolean mirrorCueSeen;
    private int mirrorCueId=-1;
    private WorldPoint mirrorCueWardrobe, lastMirror;
    private long mirrorCueAt, mirrorCueLastSeenAt;
    private boolean mirrorFacingWardrobe;
    private boolean deathRecovery, effectiveActions, retreating;
    private boolean barrelCutsceneObserved;
    private long barrelDialogueClosedAt;
    private boolean ellipsisContinueRetryUsed;
    private boolean directWidgetRetryUsed;
    private boolean stage20WidgetRetryUsed;
    private boolean barrelMenuRetryUsed;
    private boolean libraryClueMenuRetryUsed;
    private boolean cutPaintingMenuRetryUsed;
    private boolean paintingDelayedRetryUsed;
    private boolean paintingEventProbeUsed;
    private EventBus.Subscriber menuSubscriber;
    private String paintingReachProbe="";
    private long paintingReachAt;
    private Pending safetyInterrupted;
    private boolean graveAttempted;
    private long graveAttemptAt;
    private volatile Thread graveWorker;
    private volatile Boolean graveResult;

    public boolean run(MisthalinMysteryConfig settings, BooleanSupplier owner) {
        if(isRunning()) return true;
        config=settings; ownsInput=owner;
        long delay=Math.max(500,Math.min(2000,settings.tickDelay()));
        LOG.info("[MisthalinMystery] RUNNING_BUILD={} pid={} actionsEnabled={}",
            BUILD_NUMBER,ProcessHandle.current().pid(),settings.enableActions());
        menuSubscriber=Microbot.getEventBus().register(MenuOptionClicked.class,event -> {
            if(stopped||event.getId()!=ObjectID.MISTMYST_PAINTING) return;
            LOG.info("[MisthalinMystery] PAINTING_MENU_EVENT action={} id={} scene={},{} item={} widget={} consumed={}",
                event.getMenuAction(),event.getId(),event.getParam0(),event.getParam1(),
                event.getItemId(),event.getWidgetId(),event.isConsumed());
        },0f);
        telemetryFuture=scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if(stopped) return;
            try { status(Microbot.getClientThread().invoke((Supplier<Frame>)this::observe)); }
            catch(Exception ex) { LOG.warn("[MisthalinMystery] telemetry: {}",ex.toString()); }
        },0,2,TimeUnit.SECONDS);
        mainScheduledFuture=scheduledExecutorService.scheduleWithFixedDelay(this::tick,
            0,delay,TimeUnit.MILLISECONDS);
        return true;
    }
    public int runtimeBuild() { return BUILD_NUMBER; }
    @Override public void shutdown() {
        stopped=true; cancelRoute();
        if(menuSubscriber!=null) Microbot.getEventBus().unregister(menuSubscriber);
        if(telemetryFuture!=null) telemetryFuture.cancel(true);
        if(mainScheduledFuture!=null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow(); super.shutdown();
    }
    public Map<String,Object> quiesceForReload() {
        if(graveWorker!=null&&graveWorker.isAlive())
            throw new IllegalStateException("Grave recovery in flight; wait for proof before reload");
        if(telemetryFuture!=null) telemetryFuture.cancel(false);
        if(mainScheduledFuture!=null) mainScheduledFuture.cancel(false);
        scheduledExecutorService.shutdown();
        try {
            if(!scheduledExecutorService.awaitTermination(30,TimeUnit.SECONDS))
                throw new IllegalStateException("Misthalin Mystery tick did not quiesce");
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("reload interrupted",ex);
        }
        stopped=true; cancelRoute();
        if(menuSubscriber!=null) Microbot.getEventBus().unregister(menuSubscriber);
        Map<String,Object> saved=new HashMap<>();
        saved.put("held",held); saved.put("phase",phase); saved.put("error",error);
        saved.put("finished",finished); saved.put("bankPrepared",bankPrepared);
        saved.put("logoutSent",logoutSent); saved.put("lastVarp",lastVarp);
        saved.put("lastHp",lastHp); saved.put("failures",new HashMap<>(failures));
        saved.put("mirrorCueId",mirrorCueId); saved.put("mirrorCueWardrobe",mirrorCueWardrobe);
        saved.put("mirrorCueAt",mirrorCueAt); saved.put("mirrorCueSeen",mirrorCueSeen);
        saved.put("mirrorCueLastSeenAt",mirrorCueLastSeenAt);
        saved.put("mirrorFacingWardrobe",mirrorFacingWardrobe);
        saved.put("mirrorSignal",mirrorSignal);
        saved.put("retreating",retreating);
        saved.put("deathRecovery",deathRecovery);
        saved.put("barrelCutsceneObserved",barrelCutsceneObserved);
        saved.put("barrelDialogueClosedAt",barrelDialogueClosedAt);
        saved.put("ellipsisContinueRetryUsed",ellipsisContinueRetryUsed);
        saved.put("directWidgetRetryUsed",directWidgetRetryUsed);
        saved.put("stage20WidgetRetryUsed",stage20WidgetRetryUsed);
        saved.put("barrelMenuRetryUsed",barrelMenuRetryUsed);
        saved.put("libraryClueMenuRetryUsed",libraryClueMenuRetryUsed);
        saved.put("cutPaintingMenuRetryUsed",cutPaintingMenuRetryUsed);
        saved.put("paintingDelayedRetryUsed",paintingDelayedRetryUsed);
        saved.put("paintingEventProbeUsed",paintingEventProbeUsed);
        saved.put("graveAttempted",graveAttempted);
        saved.put("graveAttemptAt",graveAttemptAt);
        if(pending!=null) saved.put("inFlight",pending.key);
        if(route!=null) saved.put("routeInFlight",route.key);
        super.shutdown(); return saved;
    }
    @SuppressWarnings("unchecked")
    public void restoreReloadState(Map<String,Object> saved) {
        if(saved==null) return;
        held=(Boolean)saved.getOrDefault("held",false);
        phase=(String)saved.getOrDefault("phase","START");
        error=(String)saved.getOrDefault("error","");
        finished=(Boolean)saved.getOrDefault("finished",false);
        bankPrepared=(Boolean)saved.getOrDefault("bankPrepared",false);
        logoutSent=(Boolean)saved.getOrDefault("logoutSent",false);
        lastVarp=(Integer)saved.getOrDefault("lastVarp",-1);
        lastHp=(Integer)saved.getOrDefault("lastHp",-1);
        failures.putAll((Map<String,Integer>)saved.getOrDefault("failures",Map.of()));
        mirrorCueId=(Integer)saved.getOrDefault("mirrorCueId",-1);
        mirrorCueWardrobe=(WorldPoint)saved.get("mirrorCueWardrobe");
        mirrorCueAt=(Long)saved.getOrDefault("mirrorCueAt",0L);
        mirrorCueLastSeenAt=(Long)saved.getOrDefault("mirrorCueLastSeenAt",0L);
        mirrorCueSeen=(Boolean)saved.getOrDefault("mirrorCueSeen",false);
        mirrorFacingWardrobe=(Boolean)saved.getOrDefault("mirrorFacingWardrobe",false);
        mirrorSignal=(String)saved.getOrDefault("mirrorSignal","");
        retreating=(Boolean)saved.getOrDefault("retreating",false);
        deathRecovery=(Boolean)saved.getOrDefault("deathRecovery",false);
        barrelCutsceneObserved=(Boolean)saved.getOrDefault("barrelCutsceneObserved",false);
        barrelDialogueClosedAt=(Long)saved.getOrDefault("barrelDialogueClosedAt",0L);
        ellipsisContinueRetryUsed=(Boolean)saved.getOrDefault("ellipsisContinueRetryUsed",false);
        directWidgetRetryUsed=(Boolean)saved.getOrDefault("directWidgetRetryUsed",false);
        stage20WidgetRetryUsed=(Boolean)saved.getOrDefault("stage20WidgetRetryUsed",false);
        barrelMenuRetryUsed=(Boolean)saved.getOrDefault("barrelMenuRetryUsed",false);
        libraryClueMenuRetryUsed=(Boolean)saved.getOrDefault("libraryClueMenuRetryUsed",false);
        cutPaintingMenuRetryUsed=(Boolean)saved.getOrDefault("cutPaintingMenuRetryUsed",false);
        paintingDelayedRetryUsed=(Boolean)saved.getOrDefault("paintingDelayedRetryUsed",false);
        paintingEventProbeUsed=(Boolean)saved.getOrDefault("paintingEventProbeUsed",false);
        graveAttempted=(Boolean)saved.getOrDefault("graveAttempted",false);
        graveAttemptAt=(Long)saved.getOrDefault("graveAttemptAt",0L);
        if(saved.containsKey("inFlight")||saved.containsKey("routeInFlight")) {
            held=true; phase="HOLD_RELOAD_IN_FLIGHT";
            error="Reload during "+saved.getOrDefault("inFlight",saved.get("routeInFlight"))
                +"; inspect live varp, inventory, position and scene before resuming";
        }
    }

    private Frame observe() {
        Client c=Microbot.getClient();
        if(c==null) return null;
        Frame f=new Frame();
        f.game=c.getGameState(); f.loginIndex=c.getLoginIndex();
        f.world=c.getWorld(); f.canvasWidth=c.getCanvasWidth();
        if(f.game!=GameState.LOGGED_IN||c.getLocalPlayer()==null) return f;
        f.rawPos=c.getLocalPlayer().getWorldLocation();
        f.instanced=c.isInInstancedRegion();
        f.templatePos=WorldPoint.fromLocalInstance(c,c.getLocalPlayer().getLocalLocation());
        f.pos=f.instanced?f.templatePos:f.rawPos;
        f.bucketInstances=WorldPoint.toLocalInstance(c,BUCKET).toString();
        f.quest=Quest.MISTHALIN_MYSTERY.getState(c);
        f.rawVarp=c.getVarpValue(VarPlayerID.MISTMYST_MAIN);
        // The stage is bits 0-7; candle varbits 4039-4042 use bits 8-11.
        f.varp=f.rawVarp&0xff;
        for(int id:new int[]{VarbitID.MISTMYST_CANDLE4,VarbitID.MISTMYST_CANDLE3,
            VarbitID.MISTMYST_CANDLE1,VarbitID.MISTMYST_CANDLE2}) {
            VarbitComposition bit=c.getVarbit(id);
            if(bit!=null) f.candleVarbitLayout+=id+":index="+bit.getIndex()
                +" bits="+bit.getLeastSignificantBit()+"-"
                +bit.getMostSignificantBit()+";";
        }
        f.hp=c.getBoostedSkillLevel(Skill.HITPOINTS);
        f.maxHp=c.getRealSkillLevel(Skill.HITPOINTS);
        f.inCombat=Rs2Player.isInCombat();
        f.bank=Rs2Bank.isOpen();
        f.killerKnifeEquipped=Rs2Equipment.isWearing(ItemID.MISTMYST_CUTSCENE_KNIFE);
        Widget selected=c.getSelectedWidget();
        f.selectedWidgetMeta="selected="+c.isWidgetSelected()
            +" widget="+(selected==null?"null":selected.getId()
                +" item="+selected.getItemId()+" name="+selected.getName());
        List<MessageNode> recentMessages=new ArrayList<>();
        for(ChatLineBuffer buffer:c.getChatLineMap().values()) {
            if(buffer==null||buffer.getLines()==null) continue;
            for(MessageNode message:buffer.getLines()) {
                if(message==null||message.getValue()==null) continue;
                ChatMessageType type=message.getType();
                if(type==ChatMessageType.GAMEMESSAGE||type==ChatMessageType.SPAM
                    ||type==ChatMessageType.MESBOX) recentMessages.add(message);
            }
        }
        recentMessages.sort((a,b)->Integer.compare(b.getTimestamp(),a.getTimestamp()));
        StringBuilder gameMessages=new StringBuilder();
        for(int i=0;i<Math.min(6,recentMessages.size());i++) {
            MessageNode message=recentMessages.get(i);
            gameMessages.append(message.getTimestamp()).append(':')
                .append(message.getType()).append(':')
                .append(message.getValue().replace('\n',' ')).append(';');
        }
        f.recentGameMessage=gameMessages.toString();
        ItemContainer inv=c.getItemContainer(InventoryID.INVENTORY);
        f.inventoryLoaded=inv!=null&&inv.getItems()!=null;
        if(f.inventoryLoaded) for(Item item:inv.getItems())
            if(item!=null&&item.getId()>0&&item.getQuantity()>0)
                f.items.merge(item.getId(),item.getQuantity(),Integer::sum);
        if(f.bank) {
            ItemContainer bank=c.getItemContainer(InventoryID.BANK);
            f.bankContentsAvailable=bank!=null&&bank.getItems()!=null;
            if(f.bankContentsAvailable) for(Item item:bank.getItems())
                if(item!=null&&item.getId()>0&&item.getQuantity()>0)
                    f.bankItems.merge(item.getId(),item.getQuantity(),Integer::sum);
        }
        f.inDialogue=Rs2Dialogue.isInDialogue();
        f.hasContinue=Rs2Dialogue.hasContinue();
        String dialogue=Rs2Dialogue.getDialogueText();
        f.dialogue=dialogue==null?"":dialogue;
        if(f.inDialogue) f.dialogueWidgets=dialogueWidgetSnapshot(c);
        for(Widget w:Rs2Dialogue.getDialogueOptions())
            if(w!=null&&w.getText()!=null&&!w.getText().isBlank())
                f.options.add(w.getText());
        Widget piano=c.getWidget(554,20), gems=c.getWidget(555,1);
        f.pianoWidget=piano!=null&&!piano.isHidden();
        f.gemWidget=gems!=null&&!gems.isHidden();
        for(int bit:PUZZLE_BITS) f.bits.put(bit,c.getVarbitValue(bit));
        Widget promo=c.getWidget(InterfaceID.MembershipBenefitsPrompt.CONTENT);
        f.promoVisible=promo!=null&&!promo.isHidden();
        for(NPC npc:c.getNpcs()) {
            if(npc==null) continue;
            int id=npc.getId();
            if(id!=NpcID.MISTMYST_ABIGALE_LUM_VIS
                && (id<NpcID.MISTMYST_ABIGALE||id>NpcID.MISTMYST_MIRROR_MOVABLE)) continue;
            WorldPoint tile=f.instanced
                ?WorldPoint.fromLocalInstance(c,npc.getLocalLocation())
                :npc.getWorldLocation();
            if(tile!=null&&tile.distanceTo(f.pos)<=20)
                f.npcs.computeIfAbsent(id,x->new ArrayList<>()).add(tile);
        }
        if(f.island()) {
            for(TileObject o:Rs2GameObject.getAll()) {
                if(o!=null&&f.varp==60&&f.doorProbe.length()<1200) {
                    WorldPoint doorTile=f.instanced
                        ?WorldPoint.fromLocalInstance(c,o.getLocalLocation())
                        :o.getWorldLocation();
                    if(doorTile!=null&&distance(doorTile,RUBY_DOOR)<=2) {
                        ObjectComposition comp=c.getObjectDefinition(o.getId());
                        f.doorProbe+=o.getId()+"@"+doorTile+" "
                            +(comp==null?"?":comp.getName())+" "
                            +(comp==null?"?":Arrays.toString(comp.getActions()))+";";
                    }
                }
                if(o==null||o.getId()<29648||o.getId()>30156) continue;
                if(o.getId()==ObjectID.MISTMYST_PAINTING) {
                    LocalPoint local=o.getLocalLocation();
                    f.paintingType=o.getClass().getName()+" local="+local
                        +" view="+(o.getWorldView()==null?"null":o.getWorldView().getId());
                    if(local!=null&&o.getWorldView()!=null) {
                        CollisionData[] maps=o.getWorldView().getCollisionMaps();
                        int plane=o.getWorldView().getPlane();
                        if(maps!=null&&plane>=0&&plane<maps.length&&maps[plane]!=null) {
                            int[][] flags=maps[plane].getFlags();
                            int x=local.getSceneX(),y=local.getSceneY();
                            if(flags!=null&&x>0&&y>0&&x+1<flags.length
                                &&flags[x]!=null&&y+1<flags[x].length
                                &&flags[x-1]!=null&&y<flags[x-1].length
                                &&flags[x+1]!=null&&y<flags[x+1].length) {
                                f.paintingCollision="anchor="+Integer.toHexString(flags[x][y])
                                    +" E="+Integer.toHexString(flags[x+1][y])
                                    +" W="+Integer.toHexString(flags[x-1][y])
                                    +" N="+Integer.toHexString(flags[x][y+1])
                                    +" S="+Integer.toHexString(flags[x][y-1]);
                                f.paintingWestOpen=(flags[x][y]&CollisionDataFlag.BLOCK_MOVEMENT_WEST)==0
                                    &&(flags[x-1][y]&CollisionDataFlag.BLOCK_MOVEMENT_EAST)==0;
                            }
                        }
                    }
                    if(o instanceof DecorativeObject) {
                        DecorativeObject decoration=(DecorativeObject)o;
                        f.paintingType+=" config="+decoration.getConfig()
                            +" offsets="+decoration.getXOffset()+","+decoration.getYOffset();
                    }
                }
                WorldPoint tile=f.instanced
                    ?WorldPoint.fromLocalInstance(c,o.getLocalLocation())
                    :o.getWorldLocation();
                if(f.varp==50&&tile!=null&&distance(tile,SHELVES)<=3
                    &&f.shelfProbe.length()<1200) {
                    ObjectComposition comp=c.getObjectDefinition(o.getId());
                    String name=comp==null?"?":comp.getName();
                    String actions=comp==null?"?":Arrays.toString(comp.getActions());
                    f.shelfProbe+=o.getId()+"@"+tile+" "+name+" "+actions+";";
                }
                if(tile!=null)
                    f.objects.computeIfAbsent(o.getId(),x->new ArrayList<>()).add(tile);
            }
            if(f.boss()) for(GraphicsObject g:c.getGraphicsObjects()) {
                WorldPoint tile=g==null||g.getLocation()==null?null
                    :(f.instanced?WorldPoint.fromLocalInstance(c,g.getLocation())
                        :WorldPoint.fromLocal(c,g.getLocation()));
                if(tile!=null&&tile.distanceTo(f.pos)<=15)
                    f.graphics.add(new Graphic(g.getId(),tile));
            }
        }
        return f;
    }
    private static String dialogueWidgetSnapshot(Client client) {
        StringBuilder out=new StringBuilder();
        int[] groups={WidgetID.DIALOG_NPC_GROUP_ID,WidgetID.DIALOG_PLAYER_GROUP_ID,
            WidgetID.DIALOG_SPRITE_GROUP_ID};
        for(int group:groups) for(int child=0;child<32;child++) {
            Widget w=client.getWidget(group,child);
            if(w==null||w.isHidden()) continue;
            String label=w.getText()==null?"":w.getText();
            String name=w.getName()==null?"":w.getName();
            if(label.isBlank()&&name.isBlank()&&w.getModelId()<0) continue;
            out.append(group).append(':').append(child).append('#').append(w.getId())
                .append(" type=").append(w.getType()).append(" model=").append(w.getModelId())
                .append(" anim=").append(w.getAnimationId())
                .append(" text=").append(label,0,Math.min(90,label.length()))
                .append(" name=").append(name,0,Math.min(45,name.length())).append(';');
            if(out.length()>1600) return out.substring(0,1600);
        }
        return out.toString();
    }

    private void tick() {
        Frame f=null;
        try {
            if(stopped||Thread.currentThread().isInterrupted()) return;
            f=Microbot.getClientThread().invoke((Supplier<Frame>)this::observe);
            if(f==null) { phase="WAIT_CLIENT"; return; }
            if(f.game==GameState.LOGGED_IN&&f.quest==QuestState.FINISHED) {
                if(!finished) {
                    LOG.info("[MisthalinMystery] COMPLETE_PROVED build={} pid={} varp={} pos={}",
                        BUILD_NUMBER,ProcessHandle.current().pid(),f.varp,f.pos);
                    try { Files.createDirectories(COMPLETE_MARKER.getParent());
                        Files.writeString(COMPLETE_MARKER,"QuestState.FINISHED build="
                            +BUILD_NUMBER+" time="+System.currentTimeMillis()); }
                    catch(Exception ex) { LOG.warn("[MisthalinMystery] completion marker: {}",ex.toString()); }
                }
                finished=true; pending=null; cancelRoute(); held=false; error="";
            }
            if(!finished&&Files.isRegularFile(COMPLETE_MARKER)
                &&f.game==GameState.LOGGED_IN&&f.quest!=QuestState.FINISHED) {
                Files.deleteIfExists(COMPLETE_MARKER);
                LOG.info("[MisthalinMystery] STALE_COMPLETION_MARKER_CLEARED quest={} pos={}",
                    f.quest,f.pos);
            }
            if(finished||Files.isRegularFile(COMPLETE_MARKER)) {
                finished=true; phase="COMPLETE_QUEST_STATE";
                if(f.game==GameState.LOGGED_IN&&config.logoutOnCompletion()&&!logoutSent) {
                    logoutSent=true; Rs2Player.logout();
                    phase="VERIFY_COMPLETION_LOGOUT";
                    LOG.info("[MisthalinMystery] SAFE_LOGOUT_DISPATCH after QuestState.FINISHED");
                } else if(f.game!=GameState.LOGGED_IN&&logoutSent)
                    phase="COMPLETE_LOGGED_OUT";
                return;
            }
            effectiveActions=actionsEnabled();
            if(!effectiveActions) { phase="PREFLIGHT_STATUS_ONLY"; return; }
            if(ownsInput!=null&&!ownsInput.getAsBoolean()) {
                cancelRoute(); phase="YIELD_OTHER_PLUGIN"; return;
            }
            if(f.game!=GameState.LOGGED_IN) { loginTick(f); return; }
            WelcomeScreenEvent welcome=new WelcomeScreenEvent();
            if(welcome.validate()) {
                phase="VERIFY_WELCOME_DISMISS";
                if(welcomeAttempts++==0) {
                    welcomeAt=System.currentTimeMillis(); welcome.execute();
                    LOG.info("[MisthalinMystery] WELCOME_DISMISS_DISPATCH");
                } else if(System.currentTimeMillis()-welcomeAt>12000)
                    hold("Welcome screen persisted after native dismiss",f);
                return;
            }
            welcomeAttempts=0;
            if(f.promoVisible) {
                Widget close=Microbot.getClientThread().invoke((Supplier<Widget>)()
                    -> Microbot.getClient().getWidget(
                        InterfaceID.MembershipBenefitsPrompt.CLOSE));
                if(close==null||close.isHidden()) {
                    hold("Membership promo visible without native close widget",f); return;
                }
                issue("CLOSE_MEMBERSHIP_PROMO",Proof.PROMO_CLOSED,f,0,0,0,null,8000,
                    () -> Rs2Widget.clickWidget(close));
                return;
            }
            if(!f.inventoryLoaded) { phase="WAIT_RAW_INVENTORY"; return; }
            if(LoginManager.isMemberWorld(f.world)) {
                hold("F2P quest cannot start on member world "+f.world,f); return;
            }
            if(held && error.startsWith("Unproved SEARCH_BARREL_FIRST after ")
                && f.varp==15 && f.instanced && f.pos!=null
                && distance(f.pos,BARREL)<=3 && f.hp==f.maxHp
                && f.hasContinue && f.inDialogue
                && ("Woo, party on bro!".equals(f.dialogue)
                    || f.dialogue.startsWith("Woah, that wind"))) {
                LOG.info("[MisthalinMystery] BARREL_INSTANCE_DIALOGUE_PROVED raw={} template={}",
                    f.rawPos,f.pos);
                held=false; error=""; pending=null; barrelCutsceneObserved=true;
            }
            if(held && error.startsWith("Unproved DIALOGUE_CONTINUE_15 after 1 dispatch")
                && !directWidgetRetryUsed
                && f.varp==15 && f.instanced && f.pos!=null
                && distance(f.pos,BARREL)<=3 && f.hp==f.maxHp
                && f.inDialogue && f.hasContinue && "...".equals(f.dialogue)
                && f.dialogueWidgets.contains("231:5#")
                && f.dialogueWidgets.contains("231:4#")) {
                directWidgetRetryUsed=true;
                LOG.info("[MisthalinMystery] DIRECT_WIDGET_ELLIPSIS_ONCE pos={} widgets={}",f.pos,f.dialogueWidgets);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved DIALOGUE_CONTINUE_20 after 1 dispatch")
                && !stage20WidgetRetryUsed && f.varp==20 && f.instanced
                && f.pos!=null && distance(f.pos,BARREL)<=3 && f.hp==f.maxHp
                && f.inDialogue && f.hasContinue
                && (f.dialogueWidgets.contains("217:5#")
                    || f.dialogueWidgets.contains("231:5#"))) {
                stage20WidgetRetryUsed=true;
                LOG.info("[MisthalinMystery] DIRECT_WIDGET_STAGE20_ONCE widgets={}",f.dialogueWidgets);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved EMPTY_BARREL after 1 dispatch")
                && !barrelMenuRetryUsed && f.varp==20 && !f.instanced
                && f.pos!=null && distance(f.pos,BARREL)<=3
                && f.count(ItemID.BUCKET_EMPTY)==1 && f.hp==f.maxHp) {
                barrelMenuRetryUsed=true;
                LOG.info("[MisthalinMystery] BARREL_MENU_ALTERNATE_ONCE pos={}",f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved TRY_PINK_DOOR after 1 dispatch")
                && f.varp==30 && f.island() && f.hp==f.maxHp
                && f.inDialogue && f.hasContinue
                && f.dialogueWidgets.contains("231:4#")
                && f.dialogueWidgets.contains("text=Tayten")
                && "Gurgle...".equals(f.dialogue)) {
                LOG.info("[MisthalinMystery] PINK_DOOR_TAYTEN_DIALOGUE_PROVED pos={}",f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved TAKE_LIBRARY_CLUE after 1 dispatch")
                && !libraryClueMenuRetryUsed && f.varp==35 && f.island()
                && f.count(ItemID.MISTMYST_CLUE_LIBRARY)==0 && f.hp==f.maxHp
                && f.pos!=null && distance(f.pos,NOTE1)<=6) {
                libraryClueMenuRetryUsed=true;
                LOG.info("[MisthalinMystery] LIBRARY_CLUE_MENU_ALTERNATE_ONCE pos={}",f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved CUT_PAINTING after 1 dispatch")
                && !cutPaintingMenuRetryUsed && f.varp==40 && f.island()
                && f.count(ItemID.KNIFE)==1 && f.hp==f.maxHp
                && f.pos!=null && distance(f.pos,PAINTING)<=6) {
                cutPaintingMenuRetryUsed=true;
                LOG.info("[MisthalinMystery] CUT_PAINTING_MENU_ALTERNATE_ONCE pos={}",f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved CUT_PAINTING after 2 dispatch")
                && !paintingDelayedRetryUsed && f.varp==40 && f.island()
                && f.count(ItemID.KNIFE)==1 && f.hp==f.maxHp
                && f.pos!=null && distance(f.pos,PAINTING)<=6) {
                paintingDelayedRetryUsed=true;
                LOG.info("[MisthalinMystery] CUT_PAINTING_DELAYED_SELECTION_ONCE pos={}",f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved CUT_PAINTING after 3 dispatch")
                && !paintingEventProbeUsed && f.varp==40 && f.island()
                && f.count(ItemID.KNIFE)==1 && f.hp==f.maxHp
                && f.pos!=null && distance(f.pos,PAINTING)<=6) {
                paintingEventProbeUsed=true;
                LOG.info("[MisthalinMystery] CUT_PAINTING_EVENT_PROBE_ONCE pos={}",f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unproved CUT_PAINTING after 4 dispatch")
                && f.varp==40 && f.island() && f.paintingWestOpen
                && f.count(ItemID.KNIFE)==1 && f.hp==f.maxHp
                && f.pos!=null && distance(f.pos,PAINTING)<=6) {
                LOG.info("[MisthalinMystery] PAINTING_REPOSITION_WEST_ONCE from={} collision={}",
                    f.pos,f.paintingCollision);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("aat:") && f.varp==45
                && f.count(ItemID.MISTMYST_RUBY_KEY)==1
                && f.pos!=null && distance(f.pos,RUBY_DOOR)<=4
                && f.hp==f.maxHp && !f.inDialogue) {
                LOG.info("[MisthalinMystery] RUBY_DOOR_DEFINITION_FALLBACK_ONCE pos={}",f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Object has none of expected actions SEARCH_TINDERBOX")
                && f.varp==50 && f.count(ItemID.TINDERBOX)==0
                && f.shelfProbe.contains("30146@")
                && f.shelfProbe.contains("Take-tinderbox")
                && f.hp==f.maxHp && f.pos!=null && distance(f.pos,SHELVES)<=7) {
                LOG.info("[MisthalinMystery] SHELF_LIVE_ACTION_PROVED {}",f.shelfProbe);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Object absent LEAVE_EXPLOSION_ROOM")
                && f.varp>=65 && f.quest==QuestState.IN_PROGRESS
                && f.hp==f.maxHp) {
                LOG.info("[MisthalinMystery] EXPLOSION_STAGE_PROVED stage={} pos={}",f.varp,f.pos);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Unmapped Misthalin Mystery varp 2098")
                && f.rawVarp==2098 && f.varp==50
                && f.bit(VarbitID.MISTMYST_CANDLE4)==1
                && f.count(ItemID.TINDERBOX)==1 && f.hp==f.maxHp
                && f.island()) {
                LOG.info("[MisthalinMystery] PACKED_CANDLE_STAGE_PROVED raw={} stage={}",
                    f.rawVarp,f.varp);
                held=false; error=""; pending=null;
            }
            if(held && error.startsWith("Reload during SEARCH_BARREL_FIRST")
                && f.varp==15 && f.instanced && f.pos!=null
                && distance(f.pos,BARREL)<=3 && f.hasContinue && f.inDialogue) {
                LOG.info("[MisthalinMystery] BARREL_RELOAD_DIALOGUE_PROVED template={}",f.pos);
                held=false; error=""; pending=null; barrelCutsceneObserved=true;
            }
            if(held && error.startsWith("Route TALK_ABIGALE segment ended without progress")
                && f.varp==0 && p(3222,3219).equals(f.pos) && f.hp==f.maxHp
                && foodCount(f)>=4 && pending==null) {
                LOG.info("[MisthalinMystery] RESUME_VERIFIED_LUMBRIDGE_TELEPORT pos={} prior={}", f.pos,error);
                held=false; error="";
            }
            if(held && error.startsWith("Barrel cutscene dialogue ended but quest remained varp15")
                && f.varp>=20 && f.quest==QuestState.IN_PROGRESS) {
                LOG.info("[MisthalinMystery] BARREL_CUTSCENE_STAGE_PROVED varp={}",f.varp);
                held=false; error="";
            }
            if(lastVarp!=f.varp) {
                LOG.info("[MisthalinMystery] STAGE varp={} previous={} pos={} hp={}/{}",
                    f.varp,lastVarp,f.pos,f.hp,f.maxHp);
                lastVarp=f.varp; stageAt=System.currentTimeMillis();
                failures.clear();
                if(f.varp!=15) { barrelCutsceneObserved=false; barrelDialogueClosedAt=0; }
                if(route!=null) cancelRoute();
            }
            if(f.hp<=0) {
                pending=null; cancelRoute(); bankPrepared=false; deathRecovery=true;
                held=false; error=""; retreating=false;
                graveAttempted=false; graveResult=null;
                phase="WAIT_DEATH_RECOVERY"; return;
            }
            if(lastHp>f.hp) LOG.warn("[MisthalinMystery] HP_LOSS {} -> {} stage={} pos={}",
                lastHp,f.hp,f.varp,f.pos);
            lastHp=f.hp;
            if(pending!=null&&(pending.key.startsWith("EAT_")
                ||"CLOSE_BANK_FOR_FOOD".equals(pending.key))) {
                verifyPending(f); return;
            }
            if(f.hp>0&&f.hp<=Math.min(7,f.maxHp-1)&&foodCount(f)>0) {
                if(pending!=null) { safetyInterrupted=pending; pending=null; }
                cancelRoute();
                eat(f); return;
            }
            if(f.hp>0&&f.hp<=7&&foodCount(f)==0&&!retreating&&f.island()) {
                if(pending!=null) {
                    LOG.warn("[MisthalinMystery] RETREAT_INTERRUPTS {} hp={}",pending.key,f.hp);
                    pending=null;
                }
                cancelRoute(); retreating=true; bankPrepared=false;
                safetyInterrupted=null;
                if(held) { held=false; error=""; }
                retreatTick(f); return;
            }
            if(held) { phase="HOLD"; return; }
            if(pending!=null) { verifyPending(f); return; }
            if(safetyInterrupted!=null) {
                if(proved(safetyInterrupted,f)) {
                    LOG.info("[MisthalinMystery] SAFETY_INTERRUPTED_ACTION_PROVED {}",
                        safetyInterrupted.key);
                    safetyInterrupted=null;
                } else if(System.currentTimeMillis()-safetyInterrupted.at
                    >safetyInterrupted.timeout) {
                    hold("Safety meal interrupted unproved action "
                        +safetyInterrupted.key+"; inspect before replay",f); return;
                } else { phase="VERIFY_INTERRUPTED_ACTION"; return; }
            }
            if(route!=null) { routeTick(f); return; }
            if(System.currentTimeMillis()<nextActionAt) { phase="WAIT_PACE"; return; }
            if(retreating) { retreatTick(f); return; }
            if(deathRecovery&&graveTick(f)) return;
            if(f.hp<=7&&foodCount(f)==0) bankPrepared=false;
            if(!bankPrepared&&(!f.island()||f.varp<=10||deathRecovery)) {
                bankPrep(f); return;
            }
            if(!bankPrepared&&f.island()) bankPrepared=true;
            if(dialogue(f)) return;
            stage(f);
        } catch(Exception ex) {
            if(Thread.currentThread().isInterrupted()) return;
            held=true; phase="HOLD_EXCEPTION"; error=ex.toString();
            LOG.error("[MisthalinMystery] tick failed",ex);
        } finally { status(f); }
    }
    private boolean actionsEnabled() {
        if(config.enableActions()) return true;
        if(!Files.isRegularFile(CONTROL)) return false;
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(CONTROL)) { p.load(in); }
        catch(Exception ex) { LOG.warn("[MisthalinMystery] control read: {}",ex.toString()); return false; }
        return "true".equalsIgnoreCase(p.getProperty("enableActions","false"))
            &&Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
            &&Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"));
    }

    private void loginTick(Frame f) {
        long now=System.currentTimeMillis();
        if(f.game!=GameState.LOGIN_SCREEN) { phase="WAIT_LOGIN_SCREEN"; return; }
        if(f.loginIndex==24) {
            if(disconnectAttempts++==0&&f.canvasWidth>0) {
                LOG.info("[MisthalinMystery] DISCONNECT_MODAL_DISMISS loginIndex=24");
                Microbot.getClientThread().invoke(() -> {
                    Microbot.getMouse().click(365+(f.canvasWidth-804)/2,308); return true;
                });
                disconnectAt=now; phase="VERIFY_DISCONNECT_DISMISS";
            } else if(now-disconnectAt>8000)
                hold("Disconnected modal remained after native dismiss",f);
            return;
        }
        if(disconnectAttempts>0) {
            disconnectAttempts=0; loginAttempts=0;
            phase="DISCONNECT_DISMISSED"; return;
        }
        if(f.loginIndex!=10&&f.loginIndex!=34) {
            phase="WAIT_LOGIN_INDEX_"+f.loginIndex; return;
        }
        if(selectedWorld==0) {
            selectedWorld=LoginManager.getRandomWorld(false);
            if(selectedWorld<=0||LoginManager.isMemberWorld(selectedWorld)) {
                hold("No verified ordinary free world from LoginManager",f); return;
            }
        }
        if(loginAttempts++==0) {
            loginAt=now; phase="VERIFY_NATIVE_LOGIN";
            LOG.info("[MisthalinMystery] NATIVE_LOGIN_DISPATCH world={} index={}",
                selectedWorld,f.loginIndex);
            if(!LoginManager.login(selectedWorld))
                hold("Native LoginManager.login rejected",f);
        } else if(now-loginAt>20000)
            hold("Native login did not reach game; index="+f.loginIndex,f);
    }

    private boolean dialogue(Frame f) {
        if(!f.inDialogue&&!f.hasContinue&&f.options.isEmpty()) return false;
        if(!f.options.isEmpty()) {
            for(String option:f.options) {
                String normalized=plain(option);
                if((f.varp<=5&&(normalized.equals("yes")||normalized.equals("yes.")))
                    || (normalized.contains("yes")&&f.varp>=120)) {
                    issue("DIALOGUE_OPTION_"+f.varp,Proof.DIALOGUE,f,0,0,0,null,8000,
                        () -> Rs2Dialogue.clickOption(option));
                    return true;
                }
            }
            if(f.varp==65||f.varp==70) {
                String option=f.options.get(0);
                issue("LACEY_CUTSCENE_CHOICE",Proof.DIALOGUE,f,0,0,0,null,8000,
                    () -> Rs2Dialogue.clickOption(option));
                return true;
            }
            hold("Unknown quest dialogue option "+f.options+" text="+f.dialogue,f);
            return true;
        }
        if(f.hasContinue) {
            issue("DIALOGUE_CONTINUE_"+f.varp,Proof.DIALOGUE,f,0,0,0,null,8000,
                () -> {
                    if((f.varp==15 && "...".equals(f.dialogue)
                        || f.varp==20 && f.dialogue.isEmpty()) && f.inDialogue) {
                        int group=f.dialogueWidgets.contains("217:5#")
                            ?WidgetID.DIALOG_PLAYER_GROUP_ID:WidgetID.DIALOG_NPC_GROUP_ID;
                        return Rs2Widget.clickWidget(group,5);
                    }
                    Rs2Dialogue.clickContinue(); return true;
                });
            return true;
        }
        phase="WAIT_DIALOGUE_SETTLE"; return true;
    }

    private void bankPrep(Frame f) {
        phase="BANK_PREP";
        if(f.pos==null) { hold("No player position for bank preparation",f); return; }
        if(!f.bank) {
            BankLocation nearest=nearestFreeBank(f.pos);
            if(nearest==null) { hold("No F2P bank route from "+f.pos,f); return; }
            WorldPoint bank=nearest.getWorldPoint();
            if(distance(f.pos,bank)>7) { route(f,"TO_SUPPLY_BANK",bank,5); return; }
            issue("OPEN_SUPPLY_BANK",Proof.BANK_OPEN,f,0,0,0,null,12000,
                Rs2Bank::openBank);
            return;
        }
        if(!f.bankContentsAvailable) { phase="WAIT_RAW_BANK"; return; }
        // Preserve all current quest keys/clues, four food, and no extra load.
        for(Map.Entry<Integer,Integer> entry:f.items.entrySet()) {
            int id=entry.getKey();
            if(QUEST_ITEMS.contains(id)||isFood(id)) continue;
            issue("DEPOSIT_"+id,Proof.ITEM_MINUS,f,id,0,0,null,9000,
                () -> Rs2Bank.depositAll(id));
            return;
        }
        if(foodCount(f)<4) {
            int deficit=4-foodCount(f);
            for(int id:FOOD) if(f.bankCount(id)>0) {
                int target=Math.min(deficit,f.bankCount(id));
                issue("WITHDRAW_FOOD_"+id,Proof.ITEM_PLUS,f,id,0,0,null,9000,
                    () -> Rs2Bank.withdrawX(id,target));
                return;
            }
            hold("No four food in inventory/bank for 11 HP island; source food first",f);
            return;
        }
        int used=0;
        for(int quantity:f.items.values()) if(quantity>0) used++;
        if(28-used<4) {
            hold("Need four free inventory slots before island; used="+used,f); return;
        }
        issue("CLOSE_SUPPLY_BANK",Proof.BANK_CLOSE,f,0,0,0,null,9000,
            Rs2Bank::closeBank);
    }

    private static boolean isFood(int id) {
        for(int food:FOOD) if(food==id) return true;
        return false;
    }
    private static int foodCount(Frame f) {
        int n=0; for(int id:FOOD) n+=f.count(id); return n;
    }
    private static BankLocation nearestFreeBank(WorldPoint pos) {
        BankLocation best=null;
        int distance=Integer.MAX_VALUE;
        for(BankLocation bank:new BankLocation[]{BankLocation.AL_KHARID,
            BankLocation.DRAYNOR_VILLAGE,BankLocation.LUMBRIDGE_FRONT,
            BankLocation.LUMBRIDGE_TOP,BankLocation.VARROCK_EAST,
            BankLocation.VARROCK_WEST}) {
            int d=distance(pos,bank.getWorldPoint());
            if(d<distance) { best=bank; distance=d; }
        }
        return best;
    }
    private void eat(Frame f) {
        if(f.bank) {
            issue("CLOSE_BANK_FOR_FOOD",Proof.BANK_CLOSE,f,0,0,0,null,8000,
                Rs2Bank::closeBank);
            return;
        }
        for(int id:FOOD) if(f.count(id)>0) {
            issue("EAT_"+id,Proof.ITEM_MINUS,f,id,0,0,null,6000,
                () -> Rs2Inventory.interact(id,"Eat"));
            return;
        }
    }

    private void verifyPending(Frame f) {
        Pending p=pending;
        if("SEARCH_BARREL_FIRST".equals(p.key) && f.varp==15
            && f.instanced && f.pos!=null && distance(f.pos,BARREL)<=3
            && f.inDialogue && f.hasContinue) {
            barrelCutsceneObserved=true; pending=null;
            phase="BARREL_CUTSCENE_DIALOGUE";
            LOG.info("[MisthalinMystery] BARREL_CUTSCENE_DIALOGUE stage={} pos={} text={}",
                f.varp,f.pos,f.dialogue);
            return;
        }
        if(proved(p,f)) {
            LOG.info("[MisthalinMystery] PROVED {} varp={} pos={} hp={}",
                p.key,f.varp,f.pos,f.hp);
            if("CLOSE_SUPPLY_BANK".equals(p.key)) {
                bankPrepared=true; deathRecovery=false;
            }
            if(p.key.startsWith("PUSH_MIRROR_")&&mirrorCueWardrobe!=null) {
                WorldPoint moved=f.npc(NpcID.MISTMYST_MIRROR_MOVABLE);
                mirrorFacingWardrobe=moved!=null
                    &&(moved.getX()==mirrorCueWardrobe.getX()
                        ||moved.getY()==mirrorCueWardrobe.getY())
                    &&distance(moved,mirrorCueWardrobe)
                        <distance(p.before.npc(NpcID.MISTMYST_MIRROR_MOVABLE),
                            mirrorCueWardrobe);
            }
            pending=null; failures.remove(p.key);
            nextActionAt=System.currentTimeMillis()+280;
            return;
        }
        if(System.currentTimeMillis()-p.at<p.timeout) {
            phase="VERIFY_"+p.key; return;
        }
        pending=null;
        int count=failures.merge(p.key,1,Integer::sum);
        Rs2Walker.recalculatePath();
        hold("Unproved "+p.key+" after "+count+" dispatch; varp="+f.varp
            +" pos="+f.pos+" dialogue="+f.dialogueSignature()
            +" item="+p.item+" count="+f.count(p.item),f);
    }
    private static boolean proved(Pending p, Frame f) {
        if(f.quest==QuestState.FINISHED||f.varp>p.before.varp) return true;
        switch(p.proof) {
            case STAGE: return f.varp!=p.before.varp;
            case ITEM_PLUS: return f.count(p.item)>p.before.count(p.item);
            case ITEM_MINUS: return f.count(p.item)<p.before.count(p.item);
            case BIT_PLUS: return f.bit(p.bit)>p.before.bit(p.bit);
            case BIT_CHANGE: return f.bit(p.bit)!=p.before.bit(p.bit);
            case DIALOGUE: return !f.dialogueSignature().equals(p.before.dialogueSignature());
            case WIDGET_OPEN: return f.pianoWidget&&!p.before.pianoWidget
                || f.gemWidget&&!p.before.gemWidget;
            case WIDGET_CLOSE: return p.before.pianoWidget&&!f.pianoWidget
                || p.before.gemWidget&&!f.gemWidget;
            case POS_CHANGE: return f.pos!=null&&!f.pos.equals(p.before.pos)
                && (p.target==null||distance(f.pos,p.target)<distance(p.before.pos,p.target));
            case ISLAND: return f.island()&&!p.before.island();
            case OUTSIDE: return f.outside()&&!p.before.outside();
            case BOSS: return f.boss()&&!p.before.boss();
            case INSIDE: return p.before.outside()&&!f.outside();
            case MIRROR_MOVE:
                return f.npc(NpcID.MISTMYST_MIRROR_MOVABLE)!=null
                    && !f.npc(NpcID.MISTMYST_MIRROR_MOVABLE).equals(
                        p.before.npc(NpcID.MISTMYST_MIRROR_MOVABLE));
            case EQUIPPED: return f.killerKnifeEquipped;
            case BANK_OPEN: return f.bank&&!p.before.bank;
            case BANK_CLOSE: return !f.bank&&p.before.bank;
            case PROMO_CLOSED: return !f.promoVisible&&p.before.promoVisible;
            case BOSS_EXIT: return p.before.boss()&&!f.boss();
            case MAINLAND: return p.before.island()&&!f.island();
            case LOGGED_OUT: return f.game!=GameState.LOGGED_IN;
            case OBJECT_CHANGE: return p.target!=null
                && f.objectAt(p.object,p.target)!=p.before.objectAt(p.object,p.target);
            case QUEST_FINISHED: return f.quest==QuestState.FINISHED;
            case HP_UP: return f.hp>p.before.hp;
            default: return false;
        }
    }
    private void issue(String key,Proof proof,Frame f,int item,int bit,int object,
                       WorldPoint target,long timeout,BooleanSupplier dispatch) {
        if(pending!=null||(held&&!key.startsWith("EAT_")
            &&!"CLOSE_BANK_FOR_FOOD".equals(key))) return;
        boolean accepted=dispatch.getAsBoolean();
        LOG.info("[MisthalinMystery] ACTION {} accepted={} stage={} pos={} proof={}",
            key,accepted,f.varp,f.pos,proof);
        if(!accepted) { hold("Action rejected "+key+" at "+f.pos,f); return; }
        pending=new Pending(key,proof,f,item,bit,object,target,timeout);
        phase="VERIFY_"+key;
    }
    private void hold(String reason,Frame f) {
        if(!held) LOG.warn("[MisthalinMystery] HOLD {} stage={} pos={} hp={}/{}",
            reason,f==null?-1:f.varp,f==null?null:f.pos,
            f==null?-1:f.hp,f==null?-1:f.maxHp);
        held=true; error=reason; phase="HOLD"; pending=null; cancelRoute();
    }
    private static String plain(String s) {
        return s==null?"":s.replaceAll("<[^>]+>","")
            .replace('\u2019','\'').trim().toLowerCase();
    }
    private static int distance(WorldPoint a,WorldPoint b) {
        if(a==null||b==null||a.getPlane()!=b.getPlane()) return Integer.MAX_VALUE;
        return Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getY()-b.getY()));
    }
    private void route(Frame f,String key,WorldPoint target,int radius) {
        if(target==null||f.pos==null) { hold("Route lacks position "+key,f); return; }
        if(distance(f.pos,target)<=radius) { phase="ARRIVED_"+key; return; }
        if(route!=null) { routeTick(f); return; }
        Route r=new Route(key,target,radius,f.pos);
        route=r; startRouteSegment(r);
        LOG.info("[MisthalinMystery] ROUTE_START {} {} -> {} radius={}",
            key,f.pos,target,radius);
    }
    private void startRouteSegment(Route r) {
        r.done=false; r.endedAt=0; r.segments++;
        long started=System.currentTimeMillis();
        Thread worker=new Thread(() -> {
            try {
                WorldPoint walkTarget=Microbot.getClientThread().invoke(
                    (Supplier<WorldPoint>)() -> instanceDestination(r.target));
                Rs2Walker.walkWithStateUntil(walkTarget,r.radius,
                    () -> stopped||r.cancelled||Thread.currentThread().isInterrupted()
                        ||System.currentTimeMillis()-started>15000);
            } catch(Exception ex) {
                LOG.warn("[MisthalinMystery] route {} segment {}: {}",
                    r.key,r.segments,ex.toString());
            } finally {
                r.endedAt=System.currentTimeMillis(); r.done=true;
            }
        },"MisthalinMystery-route");
        worker.setDaemon(true); r.worker=worker; worker.start();
    }
    private WorldPoint instanceDestination(WorldPoint target) {
        Client client=Microbot.getClient();
        if(client==null||!client.isInInstancedRegion()) return target;
        WorldPoint origin=client.getLocalPlayer().getWorldLocation();
        return WorldPoint.toLocalInstance(client,target).stream()
            .min(java.util.Comparator.comparingInt(p -> distance(p,origin)))
            .orElseThrow(() -> new IllegalStateException("Target absent from current instance: "+target));
    }
    private void routeTick(Frame f) {
        Route r=route;
        if(r==null) return;
        if(r.cancelled) {
            if(r.worker==null||!r.worker.isAlive()) route=null;
            else phase="WAIT_ROUTE_CANCEL";
            return;
        }
        int dist=distance(f.pos,r.target);
        if(dist<=r.radius) {
            r.cancelled=true;
            if(r.worker!=null) r.worker.interrupt();
            if(r.worker!=null&&r.worker.isAlive()) { phase="WAIT_ROUTE_ARRIVAL"; return; }
            route=null; failures.remove("ROUTE_"+r.key);
            phase="ARRIVED_"+r.key; return;
        }
        if(f.pos!=null && !f.pos.equals(r.lastPos)) {
            r.bestDistance=Math.min(r.bestDistance,dist); r.lastProgress=System.currentTimeMillis();
            r.lastPos=f.pos;
        }
        long now=System.currentTimeMillis();
        if(now-r.at>180000||now-r.lastProgress>20000) {
            r.cancelled=true;
            if(r.worker!=null) r.worker.interrupt();
            if(r.worker!=null&&r.worker.isAlive()) { phase="WAIT_ROUTE_FAILURE_CANCEL"; return; }
            route=null; Rs2Walker.recalculatePath();
            hold("Route "+r.key+" no position progress from "+r.lastPos
                +" toward "+r.target+" bestDistance="+r.bestDistance,f);
            return;
        }
        if(r.done&&(r.worker==null||!r.worker.isAlive())&&now-r.endedAt>1000) {
            if(r.segments>=10) {
                route=null;
                hold("Route "+r.key+" consumed ten bounded walker segments; at "+f.pos,f);
            } else startRouteSegment(r);
        } else phase="WALK_"+r.key;
    }
    private void cancelRoute() {
        Route r=route;
        if(r==null) return;
        r.cancelled=true;
        if(r.worker!=null) r.worker.interrupt();
        if(r.worker==null||!r.worker.isAlive()) route=null;
    }

    private TileObject objectNear(int id,WorldPoint target) {
        return Microbot.getClientThread().invoke((Supplier<TileObject>)() -> {
            Client client=Microbot.getClient();
            TileObject best=null;
            int bestDistance=Integer.MAX_VALUE;
            for(TileObject object:Rs2GameObject.getAll()) {
                if(object==null||object.getId()!=id) continue;
                WorldPoint tile=client.isInInstancedRegion()
                    ?WorldPoint.fromLocalInstance(client,object.getLocalLocation())
                    :object.getWorldLocation();
                if(tile==null) continue;
                int d=distance(tile,target);
                if(d<bestDistance) { best=object; bestDistance=d; }
            }
            return bestDistance<=3?best:null;
        });
    }
    private void object(Frame f,String key,int id,WorldPoint tile,String... preferredActions) {
        if(distance(f.pos,tile)>6) { route(f,key,tile,4); return; }
        TileObject obj=objectNear(id,tile);
        if(obj==null) { hold("Object absent "+key+" id="+id+" near "+tile,f); return; }
        String action=null;
        for(String choice:preferredActions)
            if(Rs2GameObject.hasAction(obj,choice)) { action=choice; break; }
        if(action==null&&preferredActions.length>0) {
            hold("Object has none of expected actions "+key+" id="+id
                +" options="+Arrays.toString(preferredActions),f); return;
        }
        final String chosen=action;
        long timeout=key.contains("PINK")||key.contains("BARREL_FIRST")
            ||key.contains("TREE")||key.contains("DIAMOND")
            ||key.contains("SAPPHIRE")||key.contains("EXPLOSION")?30000:11000;
        issue(key,Proof.STAGE,f,0,0,0,tile,timeout,
            () -> chosen==null?Rs2GameObject.interact(obj)
                :safeObjectMenuAction(id,tile,chosen));
    }
    private void objectWithProof(Frame f,String key,int id,WorldPoint tile,Proof proof,
                                 int item,int bit,String... preferredActions) {
        if(distance(f.pos,tile)>6) { route(f,key,tile,4); return; }
        TileObject obj=objectNear(id,tile);
        if(obj==null) { hold("Object absent "+key+" id="+id+" near "+tile,f); return; }
        String action=null;
        for(String choice:preferredActions)
            if(Rs2GameObject.hasAction(obj,choice)) { action=choice; break; }
        if(action==null&&preferredActions.length>0) {
            hold("Object has none of expected actions "+key+" id="+id
                +" options="+Arrays.toString(preferredActions),f); return;
        }
        final String chosen=action;
        long timeout=proof==Proof.BOSS||proof==Proof.BOSS_EXIT
            ||key.contains("CLIMB")?30000:11000;
        issue(key,proof,f,item,bit,id,tile,timeout,
            () -> chosen==null?Rs2GameObject.interact(obj)
                :safeObjectMenuAction(id,tile,chosen));
    }
    private boolean safeObjectMenuAction(int id,WorldPoint tile,String chosen) {
        if(chosen==null) return false;
        BarrelMenu menu=Microbot.getClientThread().invoke((Supplier<BarrelMenu>)() -> {
            Client client=Microbot.getClient();
            List<TileObject> candidates=new ArrayList<>();
            for(TileObject o:Rs2GameObject.getAll()) {
                if(o==null||o.getId()!=id) continue;
                LocalPoint local=o.getLocalLocation();
                if(local==null||!local.isInScene()||o.getWorldView()==null) continue;
                WorldPoint world=client.isInInstancedRegion()
                    ?WorldPoint.fromLocalInstance(client,local):o.getWorldLocation();
                if(world!=null&&distance(world,tile)<=2) candidates.add(o);
            }
            if(candidates.size()!=1) {
                LOG.warn("[MisthalinMystery] OBJECT_MENU_CANDIDATES count={} id={}",
                    candidates.size(),id); return null;
            }
            TileObject live=candidates.get(0);
            ObjectComposition composition=client.getObjectDefinition(id);
            if(composition==null) return null;
            ObjectComposition impostor=null;
            try { impostor=composition.getImpostor(); }
            catch(Exception ex) {
                LOG.warn("[MisthalinMystery] OBJECT_IMPOSTOR_LOOKUP_FAILED id={} {}",
                    id,ex.toString());
            }
            if(impostor!=null) composition=impostor;
            String[] actions=composition.getActions();
            if(actions==null) return null;
            int index=-1;
            for(int i=0;i<Math.min(actions.length,5);i++)
                if(chosen.equalsIgnoreCase(actions[i])) { index=i; break; }
            if(index<0) {
                LOG.warn("[MisthalinMystery] OBJECT_MENU_ACTION_MISSING id={} chosen={} actions={}",
                    id,chosen,Arrays.toString(actions)); return null;
            }
            MenuAction[] slots={MenuAction.GAME_OBJECT_FIRST_OPTION,
                MenuAction.GAME_OBJECT_SECOND_OPTION,MenuAction.GAME_OBJECT_THIRD_OPTION,
                MenuAction.GAME_OBJECT_FOURTH_OPTION,MenuAction.GAME_OBJECT_FIFTH_OPTION};
            LocalPoint local=live.getLocalLocation();
            int sx=local.getSceneX(),sy=local.getSceneY();
            if(live instanceof GameObject) {
                GameObject gameObject=(GameObject)live;
                if(gameObject.sizeX()>1) sx-=gameObject.sizeX()/2;
                if(gameObject.sizeY()>1) sy-=gameObject.sizeY()/2;
            }
            Polygon polygon;
            try { polygon=live.getCanvasTilePoly(); }
            catch(Exception ex) { return null; }
            if(polygon==null||polygon.npoints<3) return null;
            Rectangle bounds=polygon.getBounds();
            if(bounds.width<=0||bounds.height<=0||bounds.x<0||bounds.y<0
                ||bounds.getMaxX()>client.getCanvasWidth()) return null;
            int viewId=live.getWorldView().isTopLevel()?0:live.getWorldView().getId();
            NewMenuEntry entry=new NewMenuEntry().param0(sx).param1(sy)
                .opcode(slots[index].getId()).identifier(id).itemId(-1)
                .option(chosen).target(composition.getName()).worldViewId(viewId);
            LOG.info("[MisthalinMystery] OBJECT_MENU_DISPATCH id={} action={} slot={} scene={},{} view={} rect={}",
                id,chosen,index,sx,sy,viewId,bounds);
            return new BarrelMenu(entry,bounds);
        });
        if(menu==null) return false;
        Microbot.doInvoke(menu.entry,menu.clickRegion);
        return true;
    }
    private void npc(Frame f,String key,int id,WorldPoint fallback,String action) {
        WorldPoint tile=f.npc(id);
        if(tile==null) tile=fallback;
        if(distance(f.pos,tile)>7) { route(f,key,tile,5); return; }
        if(f.npc(id)==null) {
            hold("NPC absent "+key+" id="+id+" expected near "+fallback,f); return;
        }
        issue(key,Proof.DIALOGUE,f,0,0,0,tile,10000,
            () -> Rs2Npc.interact(id,action));
    }
    private void useOnObject(Frame f,String key,int item,int object,WorldPoint tile,
                             Proof proof,int bit) {
        if(f.count(item)==0) { hold("Required item missing "+key+" id="+item,f); return; }
        if(distance(f.pos,tile)>6) { route(f,key,tile,4); return; }
        TileObject target=objectNear(object,tile);
        if(target==null) {
            hold("Target object absent "+key+" id="+object+" near "+tile,f); return;
        }
        String itemName=item==ItemID.TINDERBOX?"Tinderbox"
            :item==ItemID.KNIFE?"Knife"
            :item==ItemID.BUCKET_EMPTY?"Bucket":null;
        issue(key,proof,f,item,bit,object,tile,11000,
            () -> useItemOnObjectMenu(f,item,object,tile));
    }
    private boolean useItemOnObjectMenu(Frame f,int item,int object,WorldPoint tile) {
        if(!Rs2Inventory.use(item)) return false;
        try { Thread.sleep(150); }
        catch(InterruptedException ex) { Thread.currentThread().interrupt(); return false; }
        String selection=Microbot.getClientThread().invoke((Supplier<String>)() -> {
            Client client=Microbot.getClient();
            Widget selected=client.getSelectedWidget();
            return "selected="+client.isWidgetSelected()+" widget="
                +(selected==null?"null":selected.getId()+" item="+selected.getItemId());
        });
        LOG.info("[MisthalinMystery] ITEM_USE_SELECTION item={} {}",item,selection);
        if(!Rs2Inventory.isItemSelected()) return false;
        BarrelMenu menu=Microbot.getClientThread().invoke((Supplier<BarrelMenu>)() -> {
            Client client=Microbot.getClient();
            List<TileObject> candidates=new ArrayList<>();
            for(TileObject o:Rs2GameObject.getAll()) {
                if(o==null||o.getId()!=object) continue;
                LocalPoint local=o.getLocalLocation();
                if(local==null||!local.isInScene()||o.getWorldView()==null) continue;
                WorldPoint world=client.isInInstancedRegion()
                    ?WorldPoint.fromLocalInstance(client,local):o.getWorldLocation();
                if(world!=null&&distance(world,tile)<=2) candidates.add(o);
            }
            if(candidates.size()!=1) {
                LOG.warn("[MisthalinMystery] ITEM_OBJECT_MENU_CANDIDATES count={} id={}",
                    candidates.size(),object); return null;
            }
            TileObject live=candidates.get(0);
            LocalPoint local=live.getLocalLocation();
            int sx=local.getSceneX(),sy=local.getSceneY();
            if(live instanceof GameObject) {
                GameObject gameObject=(GameObject)live;
                if(gameObject.sizeX()>1) sx-=gameObject.sizeX()/2;
                if(gameObject.sizeY()>1) sy-=gameObject.sizeY()/2;
            }
            Polygon polygon;
            try { polygon=live.getCanvasTilePoly(); }
            catch(Exception ex) {
                LOG.warn("[MisthalinMystery] BARREL_TILE_POLY {}",ex.toString());
                return null;
            }
            if(polygon==null||polygon.npoints<3) return null;
            Rectangle bounds=polygon.getBounds();
            if(bounds.width<=0||bounds.height<=0||bounds.x<0||bounds.y<0
                ||bounds.getMaxX()>f.canvasWidth) return null;
            int viewId=live.getWorldView().isTopLevel()?0:live.getWorldView().getId();
            ObjectComposition composition=client.getObjectDefinition(live.getId());
            if(composition==null) return null;
            ObjectComposition impostor=composition.getImpostor();
            if(impostor!=null) composition=impostor;
            NewMenuEntry entry=new NewMenuEntry().param0(sx).param1(sy)
                .opcode(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT.getId())
                .identifier(live.getId()).itemId(-1).option("")
                .target(composition.getName()).worldViewId(viewId);
            LOG.info("[MisthalinMystery] ITEM_OBJECT_MENU_DISPATCH item={} id={} scene={},{} view={} rect={}",
                item,live.getId(),sx,sy,viewId,bounds);
            return new BarrelMenu(entry,bounds);
        });
        if(menu==null) return false;
        Microbot.doInvoke(menu.entry,menu.clickRegion);
        return true;
    }
    private void read(Frame f,String key,int item) {
        if(f.count(item)==0) { hold("Clue absent "+key+" id="+item,f); return; }
        issue(key,Proof.STAGE,f,item,0,0,null,10000,
            () -> Rs2Inventory.interact(item,"Read"));
    }
    private void widget(Frame f,String key,int group,int child,int bit) {
        if((group==554&&!f.pianoWidget)||(group==555&&!f.gemWidget)) {
            hold("Puzzle widget absent "+key+" group="+group,f); return;
        }
        issue(key,Proof.BIT_PLUS,f,0,bit,0,null,9000,
            () -> Rs2Widget.clickWidget(group,child));
    }
    private int visibleObject(WorldPoint tile,int... ids) {
        for(int id:ids) if(objectNear(id,tile)!=null) return id;
        return ids[0];
    }
    private void stage(Frame f) {
        if(f.varp<0) { hold("Unknown Misthalin Mystery varp",f); return; }
        if(f.varp>=10&&!f.island()) {
            if(distance(f.pos,BOAT)>7) { route(f,"RETURN_TO_ISLAND_BOAT",BOAT,5); return; }
            objectWithProof(f,"BOARD_ISLAND_BOAT",ObjectID.MISTMYST_BOAT_LUMBRIDGE,
                BOAT,Proof.ISLAND,0,0,"Board","Travel");
            return;
        }
        switch(f.varp) {
            case 0: case 5:
                npc(f,"TALK_ABIGALE",NpcID.MISTMYST_ABIGALE_LUM_VIS,ABIGALE,"Talk-to"); return;
            case 10: case 15:
                if(f.count(ItemID.BUCKET_EMPTY)==0) {
                    objectWithProof(f,"TAKE_BUCKET",ObjectID.MISTMYST_EMPTY_BUCKET,
                        BUCKET,Proof.ITEM_PLUS,ItemID.BUCKET_EMPTY,0,"Take","Pick-up"); return;
                }
                if(f.varp==15&&barrelCutsceneObserved) {
                    if(barrelDialogueClosedAt==0) barrelDialogueClosedAt=System.currentTimeMillis();
                    if(System.currentTimeMillis()-barrelDialogueClosedAt>10000)
                        hold("Barrel cutscene dialogue ended but quest remained varp15; inspect scene before another Search",f);
                    else phase="WAIT_BARREL_STAGE_AFTER_DIALOGUE";
                    return;
                }
                object(f,"SEARCH_BARREL_FIRST",visibleObject(BARREL,
                    ObjectID.MISTMYST_BARREL,ObjectID.MISTMYST_BARREL_WATER),
                    BARREL,"Search"); return;
            case 20:
                useOnObject(f,"EMPTY_BARREL",ItemID.BUCKET_EMPTY,visibleObject(BARREL,
                    ObjectID.MISTMYST_BARREL,ObjectID.MISTMYST_BARREL_WATER),
                    BARREL,Proof.STAGE,0); return;
            case 25:
                if(f.count(ItemID.MISTMYST_FRONTDOOR_KEY)==0) {
                    objectWithProof(f,"SEARCH_BARREL_KEY",visibleObject(BARREL,
                        ObjectID.MISTMYST_BARREL,ObjectID.MISTMYST_BARREL_EMPTIED),
                        BARREL,Proof.ITEM_PLUS,ItemID.MISTMYST_FRONTDOOR_KEY,0,"Search"); return;
                }
                object(f,"ENTER_MANOR",visibleObject(FRONT_DOOR,
                    ObjectID.MISTMYST_FRONT_DOORL,ObjectID.MISTMYST_FRONT_DOORR),
                    FRONT_DOOR,"Open","Enter"); return;
            case 30:
                if(f.count(ItemID.KNIFE)==0) {
                    objectWithProof(f,"TAKE_TABLE_KNIFE",ObjectID.MISTMYST_TABLE_KNIFE,
                        TABLE_KNIFE,Proof.ITEM_PLUS,ItemID.KNIFE,0,"Take-knife","Take"); return;
                }
                object(f,"TRY_PINK_DOOR",ObjectID.MISTMYST_DOOR_REDTOPAZ,
                    PINK_DOOR,"Open"); return;
            case 35:
                if(f.count(ItemID.MISTMYST_CLUE_LIBRARY)==0) {
                    objectWithProof(f,"TAKE_LIBRARY_CLUE",visibleObject(NOTE1,
                        ObjectID.MISTMYST_CLUE_LIBRARY,ObjectID.MISTMYST_CLUE_LIBRARY_VIS),
                        NOTE1,Proof.ITEM_PLUS,ItemID.MISTMYST_CLUE_LIBRARY,0,"Take","Pick-up");
                    return;
                }
                read(f,"READ_LIBRARY_CLUE",ItemID.MISTMYST_CLUE_LIBRARY); return;
            case 40:
                if(!f.paintingWestOpen) {
                    hold("Painting west edge not open in live collision map "+f.paintingCollision,f);
                    return;
                }
                if(!p(1631,4833).equals(f.pos)) {
                    route(f,"PAINTING_WEST_APPROACH",p(1631,4833),0); return;
                }
                useOnObject(f,"CUT_PAINTING",ItemID.KNIFE,visibleObject(PAINTING,
                    ObjectID.MISTMYST_PAINTING,ObjectID.MISTMYST_PAINTING_FIXED),
                    PAINTING,Proof.STAGE,0); return;
            case 45:
                if(f.count(ItemID.MISTMYST_RUBY_KEY)==0) {
                    objectWithProof(f,"SEARCH_PAINTING",visibleObject(PAINTING,
                        ObjectID.MISTMYST_PAINTING,ObjectID.MISTMYST_PAINTING_SLASHED),
                        PAINTING,Proof.ITEM_PLUS,ItemID.MISTMYST_RUBY_KEY,0,"Search");
                    return;
                }
                object(f,"ENTER_RUBY_ROOM",ObjectID.MISTMYST_DOOR_RUBY,
                    RUBY_DOOR,"Open"); return;
            case 50: candles(f); return;
            case 55:
                if(f.count(ItemID.TINDERBOX)==0) {
                    objectWithProof(f,"SEARCH_TINDERBOX",ObjectID.MISTMYST_SHELVES_TINDERBOX,
                        SHELVES,Proof.ITEM_PLUS,ItemID.TINDERBOX,0,"Take-tinderbox","Search"); return;
                }
                useOnObject(f,"LIGHT_EXPLOSIVE_BARREL",ItemID.TINDERBOX,
                    visibleObject(EXPLOSIVE_BARREL,ObjectID.MISTMYST_EXPLOSIVE_BARREL,
                        ObjectID.MISTMYST_EXPLOSIVE_BARREL_VIS),
                    EXPLOSIVE_BARREL,Proof.STAGE,0); return;
            case 60:
                object(f,"LEAVE_EXPLOSION_ROOM",ObjectID.MISTMYST_DOOR_RUBY,
                    RUBY_DOOR,"Open"); return;
            case 65:
                if(!f.outside()) {
                    objectWithProof(f,"CLIMB_DAMAGED_WALL",
                        visibleObject(WALL,ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE,
                            ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE_BROKEN),
                        WALL,Proof.OUTSIDE,0,0,"Climb-over","Climb");
                    return;
                }
                object(f,"OBSERVE_TREE",ObjectID.MISTMYST_TREE,
                    TREE,"Observe","Look-through"); return;
            case 70:
                if(!f.outside()) {
                    objectWithProof(f,"CLIMB_TO_OUTSIDE",
                        visibleObject(WALL,ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE,
                            ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE_BROKEN),
                        WALL,Proof.OUTSIDE,0,0,"Climb-over","Climb"); return;
                }
                if(f.count(ItemID.MISTMYST_CLUE_OUTSIDE)==0) {
                    objectWithProof(f,"TAKE_OUTSIDE_CLUE",
                        visibleObject(NOTE2,ObjectID.MISTMYST_CLUE_OUTSIDE,
                            ObjectID.MISTMYST_CLUE_OUTSIDE_VIS),
                        NOTE2,Proof.ITEM_PLUS,ItemID.MISTMYST_CLUE_OUTSIDE,0,
                        "Take","Pick-up"); return;
                }
                read(f,"READ_OUTSIDE_CLUE",ItemID.MISTMYST_CLUE_OUTSIDE); return;
            case 75: piano(f); return;
            case 80:
                if(f.outside()) {
                    if(f.count(ItemID.MISTMYST_EMERALD_KEY)==0) {
                        objectWithProof(f,"SEARCH_PIANO_FOR_KEY",
                            visibleObject(PIANO,ObjectID.MISTMYST_PIANO,
                                ObjectID.MISTMYST_PIANO_OPEN),
                            PIANO,Proof.ITEM_PLUS,ItemID.MISTMYST_EMERALD_KEY,0,"Search");
                        return;
                    }
                    objectWithProof(f,"RETURN_OVER_WALL",
                        visibleObject(WALL,ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE,
                            ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE_BROKEN),
                        WALL,Proof.INSIDE,0,0,"Climb-over","Climb"); return;
                }
                if(f.count(ItemID.MISTMYST_EMERALD_KEY)==0) {
                    hold("Emerald key missing after returning over wall",f); return;
                }
                object(f,"OPEN_EMERALD_DOOR",ObjectID.MISTMYST_DOOR_EMERALD,
                    EMERALD_DOOR,"Open"); return;
            case 85:
                object(f,"TRY_DIAMOND_DOOR",ObjectID.MISTMYST_DOOR_DIAMOND,
                    DIAMOND_DOOR,"Open"); return;
            case 90:
                if(f.count(ItemID.MISTMYST_CLUE_KITCHEN)==0) {
                    objectWithProof(f,"TAKE_KITCHEN_CLUE",
                        visibleObject(NOTE3,ObjectID.MISTMYST_CLUE_KITCHEN,
                            ObjectID.MISTMYST_CLUE_KITCHEN_VIS),
                        NOTE3,Proof.ITEM_PLUS,ItemID.MISTMYST_CLUE_KITCHEN,0,
                        "Take","Pick-up"); return;
                }
                read(f,"READ_KITCHEN_CLUE",ItemID.MISTMYST_CLUE_KITCHEN); return;
            case 95:
                useOnObject(f,"CUT_FIREPLACE",ItemID.KNIFE,
                    visibleObject(FIREPLACE,ObjectID.MISTMYST_FIREPLACE,
                        ObjectID.MISTMYST_FIREPLACE_UNLIT),
                    FIREPLACE,Proof.STAGE,0); return;
            case 100: gems(f); return;
            case 105:
                if(f.count(ItemID.MISTMYST_SAPPHIRE_KEY)==0) {
                    objectWithProof(f,"SEARCH_FIREPLACE_FOR_KEY",
                        visibleObject(FIREPLACE,ObjectID.MISTMYST_FIREPLACE,
                            ObjectID.MISTMYST_FIREPLACE_REVEALED),
                        FIREPLACE,Proof.ITEM_PLUS,ItemID.MISTMYST_SAPPHIRE_KEY,0,"Search");
                    return;
                }
                objectWithProof(f,"ENTER_SAPPHIRE_ROOM",ObjectID.MISTMYST_DOOR_SAPPHIRE,
                    SAPPHIRE_DOOR,Proof.BOSS,0,0,"Open"); return;
            case 110: case 111: mirror(f); return;
            case 115:
                if(!f.boss()) {
                    objectWithProof(f,"CONTINUE_SAPPHIRE_ROOM",
                        ObjectID.MISTMYST_DOOR_SAPPHIRE,SAPPHIRE_DOOR,
                        Proof.BOSS,0,0,"Open"); return;
                }
                if(System.currentTimeMillis()-stageAt>90000)
                    hold("Killer reveal cutscene did not advance varp after 90s",f);
                else phase="WAIT_KILLER_REVEAL_CUTSCENE";
                return;
            case 120: fight(f); return;
            case 125:
                object(f,"ATTEMPT_EXIT_SAPPHIRE_ROOM",ObjectID.MISTMYST_DOOR_SAPPHIRE,
                    SAPPHIRE_DOOR,"Open"); return;
            case 130:
                npc(f,"TALK_MANDY_FINISH",NpcID.MISTMYST_MANDY_POST_VIS,
                    MANDY,"Talk-to"); return;
            default:
                hold("Unmapped Misthalin Mystery varp "+f.varp,f);
        }
    }

    private void candles(Frame f) {
        if(f.count(ItemID.TINDERBOX)==0) {
            objectWithProof(f,"SEARCH_TINDERBOX",ObjectID.MISTMYST_SHELVES_TINDERBOX,
                SHELVES,Proof.ITEM_PLUS,ItemID.TINDERBOX,0,"Take-tinderbox","Search"); return;
        }
        int[] bits={VarbitID.MISTMYST_CANDLE4,VarbitID.MISTMYST_CANDLE3,
            VarbitID.MISTMYST_CANDLE1,VarbitID.MISTMYST_CANDLE2};
        int[] objects={ObjectID.MISTMYST_CANDLE4,ObjectID.MISTMYST_CANDLE3,
            ObjectID.MISTMYST_CANDLE1,ObjectID.MISTMYST_CANDLE2};
        WorldPoint[] tiles={CANDLE1,CANDLE2,CANDLE3,CANDLE4};
        for(int i=0;i<4;i++) if(f.bit(bits[i])==0) {
            useOnObject(f,"LIGHT_CANDLE_"+(i+1),ItemID.TINDERBOX,
                visibleObject(tiles[i],objects[i],ObjectID.MISTMYST_CANDLE_UNLIT),
                tiles[i],i==3?Proof.STAGE:Proof.BIT_PLUS,bits[i]);
            return;
        }
        phase="WAIT_ALL_CANDLES_STAGE";
        if(System.currentTimeMillis()-stageAt>12000)
            hold("Four candles lit but varp did not advance",f);
    }

    private void piano(Frame f) {
        if(!f.outside()) {
            objectWithProof(f,"CLIMB_TO_PIANO_SIDE",
                visibleObject(WALL,ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE,
                    ObjectID.MISTMYST_DESTRUCTABLE_WALL_CLIMBABLE_BROKEN),
                WALL,Proof.OUTSIDE,0,0,"Climb-over","Climb"); return;
        }
        if(f.bit(VarbitID.MISTMYST_PIANO_DEAD)>0) {
            hold("Piano wrong-note flag set; inspect reset control before resuming",f); return;
        }
        if(!f.pianoWidget) {
            objectWithProof(f,"OPEN_PIANO",visibleObject(PIANO,
                ObjectID.MISTMYST_PIANO,ObjectID.MISTMYST_PIANO_CLOSED),
                PIANO,Proof.WIDGET_OPEN,0,0,"Play"); return;
        }
        int attempts=f.bit(VarbitID.MISTMYST_PIANO_ATTEMPTS);
        switch(attempts) {
            case 0: widget(f,"PIANO_D1",554,InterfaceID.MistmystPiano.LABEL_D1 & 0xffff,
                VarbitID.MISTMYST_PIANO_ATTEMPTS); return;
            case 1: widget(f,"PIANO_E",554,InterfaceID.MistmystPiano.LABEL_E1 & 0xffff,
                VarbitID.MISTMYST_PIANO_ATTEMPTS); return;
            case 2: widget(f,"PIANO_A",554,InterfaceID.MistmystPiano.LABEL_A2 & 0xffff,
                VarbitID.MISTMYST_PIANO_ATTEMPTS); return;
            case 3: widget(f,"PIANO_D2",554,InterfaceID.MistmystPiano.LABEL_D1 & 0xffff,
                VarbitID.MISTMYST_PIANO_ATTEMPTS); return;
            default:
                if(System.currentTimeMillis()-stageAt>15000)
                    hold("Piano keys played but stage unchanged attempts="+attempts,f);
                else phase="WAIT_PIANO_STAGE";
        }
    }

    private void gems(Frame f) {
        if(!f.gemWidget) {
            objectWithProof(f,"OPEN_GEM_PANEL",visibleObject(FIREPLACE,
                ObjectID.MISTMYST_FIREPLACE,ObjectID.MISTMYST_FIREPLACE_REVEALED),
                FIREPLACE,Proof.WIDGET_OPEN,0,0,"Search"); return;
        }
        int attempts=f.bit(VarbitID.MISTMYST_SWITCH_ATTEMPTS);
        int[] children={19,4,11,23,7,15};
        String[] labels={"SAPPHIRE","DIAMOND","ZENYTE","EMERALD","ONYX","RUBY"};
        if(attempts>=0&&attempts<children.length) {
            widget(f,"GEM_"+labels[attempts],555,children[attempts],
                VarbitID.MISTMYST_SWITCH_ATTEMPTS); return;
        }
        if(System.currentTimeMillis()-stageAt>15000)
            hold("Gem sequence selected but stage unchanged attempts="+attempts,f);
        else phase="WAIT_GEM_STAGE";
    }

    private void fight(Frame f) {
        if(!f.boss()) {
            objectWithProof(f,"RETURN_TO_KILLER",ObjectID.MISTMYST_DOOR_SAPPHIRE,
                SAPPHIRE_DOOR,Proof.BOSS,0,0,"Open"); return;
        }
        if(!f.killerKnifeEquipped) {
            if(f.count(ItemID.MISTMYST_CUTSCENE_KNIFE)==0) {
                if(!Rs2GroundItem.exists(ItemID.MISTMYST_CUTSCENE_KNIFE,12)) {
                    phase="WAIT_KILLER_KNIFE_GROUND";
                    if(System.currentTimeMillis()-stageAt>20000)
                        hold("Killer's knife absent on ground after reveal",f);
                    return;
                }
                issue("PICKUP_KILLER_KNIFE",Proof.ITEM_PLUS,f,
                    ItemID.MISTMYST_CUTSCENE_KNIFE,0,0,null,10000,
                    () -> Rs2GroundItem.pickup(ItemID.MISTMYST_CUTSCENE_KNIFE));
                return;
            }
            issue("EQUIP_KILLER_KNIFE",Proof.EQUIPPED,f,
                ItemID.MISTMYST_CUTSCENE_KNIFE,0,0,null,8000,
                () -> Rs2Inventory.interact(ItemID.MISTMYST_CUTSCENE_KNIFE,"Wield"));
            return;
        }
        WorldPoint target=f.npc(NpcID.MISTMYST_ABIGALE_KILLER_ATTACKABLE);
        if(target==null) { hold("Fight Abigale NPC absent after knife equipped",f); return; }
        if(distance(f.pos,target)>7) { route(f,"TO_FIGHT_ABIGALE",target,5); return; }
        issue("FIGHT_ABIGALE",Proof.STAGE,f,0,0,0,target,12000,
            () -> Rs2Npc.interact(NpcID.MISTMYST_ABIGALE_KILLER_ATTACKABLE,"Fight"));
    }

    private void mirror(Frame f) {
        if(!f.boss()) {
            objectWithProof(f,"ENTER_MIRROR_ROOM",ObjectID.MISTMYST_DOOR_SAPPHIRE,
                SAPPHIRE_DOOR,Proof.BOSS,0,0,"Open"); return;
        }
        WorldPoint movable=f.npc(NpcID.MISTMYST_MIRROR_MOVABLE);
        List<WorldPoint> wardrobes=new ArrayList<>();
        wardrobes.addAll(f.objects.getOrDefault(ObjectID.MISTMYST_BOSS_WARDROBE,List.of()));
        wardrobes.addAll(f.objects.getOrDefault(ObjectID.MISTMYST_BOSS_WARDROBE_OPEN,List.of()));
        WorldPoint open=null;
        List<WorldPoint> openList=f.objects.getOrDefault(
            ObjectID.MISTMYST_BOSS_WARDROBE_OPEN,List.of());
        if(openList.size()==1) open=openList.get(0);
        WorldPoint killer=f.npc(NpcID.MISTMYST_KILLER_BACKGROUND_VIS);
        if(killer==null) killer=f.npc(NpcID.MISTMYST_KILLER_BACKGROUND);
        if(open==null&&killer!=null)
            for(WorldPoint wardrobe:wardrobes) if(distance(killer,wardrobe)<=1) {
                open=wardrobe; break;
            }
        int graphicId=-1;
        if(open==null) {
            Map<WorldPoint,Integer> counts=new HashMap<>();
            Map<WorldPoint,Integer> ids=new HashMap<>();
            for(Graphic g:f.graphics)
                for(WorldPoint wardrobe:wardrobes)
                    if(distance(g.point,wardrobe)<=1) {
                        counts.merge(wardrobe,1,Integer::sum);
                        ids.put(wardrobe,g.id);
                    }
            if(counts.size()==1) {
                open=counts.keySet().iterator().next();
                graphicId=ids.get(open);
            }
        }
        String snapshot="mirror="+movable+" wardrobes="+wardrobes
            +" open="+open+" killer="+killer+" graphics="+f.graphics;
        mirrorSignal=snapshot;
        if(open!=null) {
            if(!open.equals(mirrorCueWardrobe)||graphicId!=mirrorCueId) {
                mirrorCueWardrobe=open; mirrorCueId=graphicId;
                mirrorCueAt=System.currentTimeMillis();
                mirrorCueLastSeenAt=mirrorCueAt;
                mirrorCueSeen=false; mirrorFacingWardrobe=false;
                LOG.info("[MisthalinMystery] MIRROR_CUE_CANDIDATE {}",snapshot);
            } else {
                mirrorCueLastSeenAt=System.currentTimeMillis();
                if(System.currentTimeMillis()-mirrorCueAt>=650) mirrorCueSeen=true;
            }
        }
        if(movable==null) {
            if(System.currentTimeMillis()-stageAt>12000)
                hold("Movable mirror NPC absent; "+snapshot,f);
            else phase="WAIT_MIRROR_NPC";
            return;
        }
        if(!mirrorCueSeen||mirrorCueWardrobe==null
            ||System.currentTimeMillis()-mirrorCueLastSeenAt>3000) {
            if(System.currentTimeMillis()-stageAt>30000)
                hold("No unique stable wardrobe telegraph after 30s; "+snapshot,f);
            else phase="WAIT_UNIQUE_MIRROR_TELEGRAPH";
            return;
        }
        int dx=mirrorCueWardrobe.getX()-movable.getX();
        int dy=mirrorCueWardrobe.getY()-movable.getY();
        if((dx==0||dy==0)&&mirrorFacingWardrobe) {
            phase="MIRROR_ALIGNED_WAIT_REFLECTION";
            if(System.currentTimeMillis()-mirrorCueAt>12000)
                hold("Mirror aligned but no verified reflection; "+snapshot,f);
            return;
        }
        // Wiki mechanic: a matching row/column is insufficient. The last
        // push must also face the mirror toward that wardrobe.
        int stepX=0,stepY=0;
        if(dx==0&&dy!=0) stepY=Integer.signum(dy);
        else if(dy==0&&dx!=0) stepX=Integer.signum(dx);
        else if(Math.abs(dx)<=Math.abs(dy)) stepX=Integer.signum(dx);
        else stepY=Integer.signum(dy);
        if(stepX==0&&stepY==0) {
            hold("Mirror overlaps wardrobe; cannot infer push side "+snapshot,f); return;
        }
        WorldPoint stand=p(movable.getX()-stepX,movable.getY()-stepY);
        if(stand.getX()<1619||stand.getX()>1627
            ||stand.getY()<4825||stand.getY()>4834) {
            hold("Required mirror push side outside boss room stand="+stand
                +" "+snapshot,f); return;
        }
        if(!f.pos.equals(stand)) {
            route(f,"MIRROR_PUSH_SIDE",stand,0); return;
        }
        if(!Rs2Npc.hasAction(NpcID.MISTMYST_MIRROR_MOVABLE,"Push")) {
            hold("Movable mirror has no Push action; "+snapshot,f); return;
        }
        lastMirror=movable;
        issue("PUSH_MIRROR_"+stepX+"_"+stepY,Proof.MIRROR_MOVE,f,0,0,0,null,8000,
            () -> Rs2Npc.interact(NpcID.MISTMYST_MIRROR_MOVABLE,"Push"));
    }

    private void retreatTick(Frame f) {
        phase="RETREAT_FOR_FOOD";
        if(!f.island()) {
            retreating=false; bankPrepared=false; deathRecovery=true;
            LOG.warn("[MisthalinMystery] RETREAT_REACHED_MAINLAND pos={} hp={}/{}",
                f.pos,f.hp,f.maxHp);
            return;
        }
        if(f.boss()) {
            objectWithProof(f,"RETREAT_FROM_BOSS_ROOM",
                ObjectID.MISTMYST_DOOR_SAPPHIRE,SAPPHIRE_DOOR,
                Proof.BOSS_EXIT,0,0,"Open");
            return;
        }
        List<WorldPoint> boats=f.objects.get(ObjectID.MISTMYST_BOAT_ISLAND);
        if(boats==null||boats.isEmpty()) {
            if(distance(f.pos,BUCKET)>8) route(f,"SEEK_ISLAND_EXIT",BUCKET,6);
            else hold("Island rowboat not loaded near fountain during retreat",f);
            return;
        }
        WorldPoint boat=boats.get(0);
        objectWithProof(f,"RETREAT_BOARD_BOAT",ObjectID.MISTMYST_BOAT_ISLAND,
            boat,Proof.MAINLAND,0,0,"Board","Travel");
    }
    private boolean graveTick(Frame f) {
        if(!Rs2Death.hasDeathToHandle()) return false;
        if(!graveAttempted) {
            graveAttempted=true; graveAttemptAt=System.currentTimeMillis();
            graveWorker=new Thread(() -> {
                try { graveResult=Rs2Death.recoverItems(); }
                catch(Exception ex) {
                    LOG.warn("[MisthalinMystery] GRAVE_RECOVERY_EXCEPTION {}",ex.toString());
                    graveResult=false;
                }
            },"MisthalinMystery-grave");
            graveWorker.setDaemon(true); graveWorker.start();
            LOG.info("[MisthalinMystery] GRAVE_RECOVERY_DISPATCH deathLocation={} grave={}",
                Rs2Death.getLastDeathLocation(),Rs2Death.hasGrave());
            phase="VERIFY_GRAVE_RECOVERY";
            return true;
        }
        if(graveWorker!=null&&graveWorker.isAlive()) {
            if(System.currentTimeMillis()-graveAttemptAt>180000)
                hold("Grave recovery still active after three minutes",f);
            else phase="VERIFY_GRAVE_RECOVERY";
            return true;
        }
        if(!Rs2Death.hasDeathToHandle()) {
            LOG.info("[MisthalinMystery] GRAVE_RECOVERY_PROVED result={} pos={}",
                graveResult,f.pos);
            return false;
        }
        if(System.currentTimeMillis()-graveAttemptAt<20000) {
            phase="WAIT_GRAVE_STATE_SETTLE"; return true;
        }
        hold("Grave recovery unproved result="+graveResult
            +" grave="+Rs2Death.hasGrave()
            +" deathLocation="+Rs2Death.getLastDeathLocation(),f);
        return true;
    }

    private synchronized void status(Frame f) {
        if(stopped||f==null) return;
        try {
            Properties p=new Properties();
            p.setProperty("timestamp",Long.toString(System.currentTimeMillis()));
            p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
            p.setProperty("build",Integer.toString(BUILD_NUMBER));
            p.setProperty("game",String.valueOf(f.game));
            p.setProperty("gameState",String.valueOf(f.game));
            p.setProperty("nativeLogin","true");
            p.setProperty("questState",String.valueOf(f.quest));
            p.setProperty("quest",String.valueOf(f.quest));
            p.setProperty("varp",Integer.toString(f.varp));
            p.setProperty("varpMisthalin",Integer.toString(f.varp));
            p.setProperty("rawVarpMisthalin",Integer.toString(f.rawVarp));
            p.setProperty("mainVarpId",Integer.toString(VarPlayerID.MISTMYST_MAIN));
            p.setProperty("candleVarbitLayout",f.candleVarbitLayout);
            p.setProperty("position",String.valueOf(f.pos));
            p.setProperty("rawPosition",String.valueOf(f.rawPos));
            p.setProperty("instanced",Boolean.toString(f.instanced));
            p.setProperty("templatePosition",String.valueOf(f.templatePos));
            p.setProperty("bucketInstanceCandidates",f.bucketInstances);
            p.setProperty("world",Integer.toString(f.world));
            p.setProperty("hp",Integer.toString(f.hp));
            p.setProperty("maxHp",Integer.toString(f.maxHp));
            p.setProperty("inCombat",Boolean.toString(f.inCombat));
            p.setProperty("inventory",f.items.toString());
            p.setProperty("items",f.items.toString());
            p.setProperty("foodCount",Integer.toString(foodCount(f)));
            p.setProperty("bankPrepared",Boolean.toString(bankPrepared));
            p.setProperty("bankOpen",Boolean.toString(f.bank));
            p.setProperty("bankContentsAvailable",Boolean.toString(f.bankContentsAvailable));
            p.setProperty("bankItems",f.bankItems.toString());
            p.setProperty("bits",f.bits.toString());
            p.setProperty("phase",phase);
            p.setProperty("held",Boolean.toString(held));
            p.setProperty("error",error);
            p.setProperty("pending",pending==null?"":pending.key);
            p.setProperty("route",route==null?"":route.key+":"+route.target);
            p.setProperty("actionsEnabled",Boolean.toString(effectiveActions));
            p.setProperty("configActionsEnabled",Boolean.toString(config.enableActions()));
            p.setProperty("completed",Boolean.toString(finished
                ||Files.isRegularFile(COMPLETE_MARKER)));
            p.setProperty("autoLoginSuppressed",Boolean.toString(finished
                ||Files.isRegularFile(COMPLETE_MARKER)));
            p.setProperty("logoutSent",Boolean.toString(logoutSent));
            p.setProperty("deathRecovery",Boolean.toString(deathRecovery));
            p.setProperty("mirrorSignal",mirrorSignal);
            p.setProperty("mirrorCueWardrobe",String.valueOf(mirrorCueWardrobe));
            p.setProperty("mirrorCueId",Integer.toString(mirrorCueId));
            p.setProperty("mirrorCueSeen",Boolean.toString(mirrorCueSeen));
            p.setProperty("mirrorFacingWardrobe",Boolean.toString(mirrorFacingWardrobe));
            p.setProperty("mirrorTile",String.valueOf(f.npc(NpcID.MISTMYST_MIRROR_MOVABLE)));
            p.setProperty("graphics",f.graphics.toString());
            p.setProperty("dialogue",f.dialogue.replace("\n"," "));
            p.setProperty("dialogueWidgets",f.dialogueWidgets);
            p.setProperty("selectedWidget",f.selectedWidgetMeta);
            p.setProperty("paintingObjects",String.valueOf(f.objects.get(ObjectID.MISTMYST_PAINTING)));
            p.setProperty("paintingType",f.paintingType);
            p.setProperty("paintingCollision",f.paintingCollision);
            p.setProperty("shelfProbe",f.shelfProbe);
            p.setProperty("doorProbe",f.doorProbe);
            p.setProperty("recentGameMessage",f.recentGameMessage);
            if(held&&f.varp==40&&System.currentTimeMillis()-paintingReachAt>30000) {
                StringBuilder reach=new StringBuilder();
                for(WorldPoint candidate:new WorldPoint[]{p(1631,4833),p(1632,4832),
                    p(1632,4834),p(1633,4832),p(1633,4834)}) {
                    try { reach.append(candidate).append('=')
                        .append(Rs2Walker.canReach(candidate)).append(';'); }
                    catch(Exception ex) { reach.append(candidate).append("=ERR:")
                        .append(ex.getClass().getSimpleName()).append(';'); }
                }
                paintingReachProbe=reach.toString();
                paintingReachAt=System.currentTimeMillis();
            }
            p.setProperty("paintingReachProbe",paintingReachProbe);
            p.setProperty("options",f.options.toString());
            Files.createDirectories(STATUS.getParent());
            Path temp=STATUS.resolveSibling("status.tmp");
            try(OutputStream out=Files.newOutputStream(temp)) {
                p.store(out,"Misthalin Mystery live status");
            }
            Files.move(temp,STATUS,StandardCopyOption.REPLACE_EXISTING);
            lastStatusAt=System.currentTimeMillis();
        } catch(Exception ex) {
            LOG.warn("[MisthalinMystery] status failed: {}",ex.toString());
        }
    }
}

