package net.runelite.client.plugins.microbot.questcommon.preparation.gear;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Serialized bank/equip pass; shared caller retains exclusive input ownership. */
public final class GearRecoveryController {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(GearRecoveryController.class);
    public interface Ui {
        GearUpgradePlanner.Snapshot observe();
        boolean openBank(); boolean closeBank();
        boolean itemMode(); boolean setItemMode();
        boolean withdrawOne(int id); boolean equipOne(int id);
    }
    public record Result(boolean complete,boolean held,String detail) { }
    private final Ui ui;
    private final Path checkpoint;
    private final BooleanSupplier ownsInput;
    private final GearUpgradePlanner.Goal goal;
    private long deadline;
    private GearUpgradePlanner planner=new GearUpgradePlanner();
    private String uiPending="",error="";
    private long uiAt;
    private int equipCandidate;
    private boolean finished;

    public GearRecoveryController(Ui ui,Path checkpoint,BooleanSupplier ownsInput,
        GearUpgradePlanner.Goal goal) throws IOException {
        this.ui=Objects.requireNonNull(ui);this.checkpoint=Objects.requireNonNull(checkpoint);
        this.ownsInput=Objects.requireNonNull(ownsInput);this.goal=Objects.requireNonNull(goal);
        deadline=goal.deadlineMillis();
        if(Files.exists(checkpoint)){
            Properties p=new Properties();try(var in=Files.newInputStream(checkpoint)){p.load(in);}
            if(!goal.accountKey().equals(p.getProperty("account")))throw new IOException("gear checkpoint account mismatch");
            if(!fingerprint().equals(p.getProperty("goalFingerprint")))throw new IOException("gear checkpoint outcome constraints changed");
            deadline=Math.min(deadline,Long.parseLong(p.getProperty("deadline")));
            planner=new GearUpgradePlanner(GearUpgradePlanner.Checkpoint.fromProperties(p));
            uiPending=p.getProperty("uiPending","");uiAt=Long.parseLong(p.getProperty("uiAt","0"));
            equipCandidate=Integer.parseInt(p.getProperty("equipCandidate","0"));
            error=p.getProperty("controllerError","");finished=Boolean.parseBoolean(p.getProperty("controllerFinished","false"));
        }
    }
    public synchronized Result tick(){
        if(!error.isEmpty())return new Result(false,true,error);
        if(!ownsInput.getAsBoolean())return new Result(false,false,"waiting for exclusive input");
        try{
            var s=ui.observe();long now=System.currentTimeMillis();
            if(s==null || !goal.accountKey().equals(s.accountKey()) || !s.loggedIn()
                || s.inCombat() || s.at()<=0 || s.at()>now || now-s.at()>3000)
                return hold("fresh same-account noncombat gear frame unavailable");
            if(!uiPending.isEmpty()){
                boolean proved=s.at()>uiAt && switch(uiPending){
                    case "OPEN" -> s.bankOpen() && s.bankAudited();
                    case "CLOSE" -> !s.bankOpen();
                    case "ITEM_MODE" -> s.bankOpen() && ui.itemMode();
                    default -> throw new IllegalStateException("unknown UI intent");
                };
                if(proved){
                    if("OPEN".equals(uiPending))LOG.info("[GearRecovery] BANK_AUDITED bank={} equipped={} eligible={}",
                        s.bank(),s.equipped(),s.verifiedItems().keySet());
                    uiPending="";save();return waitFor("bank UI transition proved");}
                if(now-uiAt>8000)return hold("unproved bank UI action; no replay: "+uiPending);
                return waitFor("verifying "+uiPending);
            }
            if(now>deadline)return hold("gear pass deadline reached");
            if(finished){
                if(s.bankOpen())return dispatchUi("CLOSE");
                return new Result(true,false,"gear pass and closed bank proved; readiness check still required");
            }
            var pending=planner.checkpoint().pending();
            var d=pending!=null || equipCandidate==0?planner.choose(goal,s)
                :planner.afterBankClosed(goal,s,equipCandidate);
            switch(d.action()){
                case WAIT: return waitFor(d.reason());
                case HOLD: return hold(d.reason());
                case COMPLETE:
                    finished=true;save();
                    if(s.bankOpen())return dispatchUi("CLOSE");
                    return new Result(true,false,"bank-only gear pass complete; readiness check still required");
                case OPEN_BANK:
                    equipCandidate=0;save();return dispatchUi("OPEN");
                case CLOSE_BANK:
                    equipCandidate=d.itemId();save();return dispatchUi("CLOSE");
                case WITHDRAW_ONE:
                    if(!ui.itemMode())return dispatchUi("ITEM_MODE");
                    planner.beginDispatch(goal,d,s);save();
                    if(!ownsInput.getAsBoolean())return hold("input lost after saved withdrawal intent");
                    LOG.info("[GearRecovery] WITHDRAW item={} slot={}",d.itemId(),d.slot());
                    ui.withdrawOne(d.itemId());return waitFor("verifying withdrawal delta");
                case EQUIP_ONE:
                    planner.beginDispatch(goal,d,s);save();
                    if(!ownsInput.getAsBoolean())return hold("input lost after saved equip intent");
                    LOG.info("[GearRecovery] EQUIP item={} slot={}",d.itemId(),d.slot());
                    ui.equipOne(d.itemId());return waitFor("verifying equipment delta");
                default: return hold("unsupported gear decision");
            }
        }catch(Exception ex){return hold("gear controller failure: "+ex);}
    }
    private Result dispatchUi(String action) throws IOException {
        uiPending=action;uiAt=System.currentTimeMillis();save();
        if(!ownsInput.getAsBoolean())return hold("input lost after saved UI intent");
        switch(action){case "OPEN"->ui.openBank();case "CLOSE"->ui.closeBank();case "ITEM_MODE"->ui.setItemMode();}
        return waitFor("verifying "+action);
    }
    private Result waitFor(String text){return new Result(false,false,text);}
    private Result hold(String text){
        error=text;try{save();}catch(Exception ignored){error=text+"; checkpoint save failed";}
        return new Result(false,true,error);
    }
    private void save() throws IOException {
        Properties p=planner.checkpoint().toProperties();p.setProperty("account",goal.accountKey());
        p.setProperty("goalFingerprint",fingerprint());p.setProperty("deadline",Long.toString(deadline));
        p.setProperty("uiPending",uiPending);p.setProperty("uiAt",Long.toString(uiAt));
        p.setProperty("equipCandidate",Integer.toString(equipCandidate));p.setProperty("controllerError",error);
        p.setProperty("controllerFinished",Boolean.toString(finished));
        Files.createDirectories(checkpoint.toAbsolutePath().getParent());
        Path temp=checkpoint.resolveSibling(checkpoint.getFileName()+".tmp");
        try(var out=Files.newOutputStream(temp)){p.store(out,"Gear action intent/proof");}
        Files.move(temp,checkpoint,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    private String fingerprint(){
        return goal.accountKey()+"|"+new TreeMap<>(goal.retain())+"|"
            +new TreeMap<>(goal.bonusWeights())+"|"+goal.minimumFreeSlots()+"|"+goal.f2pOnly();
    }
}
