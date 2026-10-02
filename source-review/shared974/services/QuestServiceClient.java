package net.runelite.client.plugins.microbot.questcommon.services;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Quest-thread endpoint; worker callbacks never execute quest gameplay. */
public final class QuestServiceClient implements QuestServiceHub.QuestOwner {
    public interface InputState {
        /** Cancel route workers; retain pending proof until reconciled. */
        void requestStop();
        /** Nonblocking: true only when all quest input workers actually stopped. */
        boolean isIdle();
    }
    public record Result(QuestServiceHub.Kind kind,QuestServiceHub.Outcome outcome,String proof) { }
    private final InputState input;
    private final AtomicReference<Result> mailbox=new AtomicReference<>();
    private volatile boolean pending;
    private QuestServiceHub.Request request;
    private Result delivered;

    public QuestServiceClient(InputState input){this.input=Objects.requireNonNull(input);}
    /** Call only from the quest tick; false means this client already has a job. */
    public boolean begin(QuestServiceHub.Request request){
        Objects.requireNonNull(request);
        if(pending)return false;
        this.request=request;delivered=null;mailbox.set(null);pending=true;
        try{
            if(QuestServiceHub.submit(request,this)==null)
                mailbox.set(new Result(request.kind(),QuestServiceHub.Outcome.UNAVAILABLE,
                    "No free registered provider for "+request.kind()));
        }catch(RuntimeException failure){
            mailbox.set(new Result(request.kind(),QuestServiceHub.Outcome.HOLD,
                "Service submission failed: "+failure));
        }
        return true;
    }
    @Override public void requestYield(){input.requestStop();}
    @Override public boolean isQuiescent(){return pending && input.isIdle();}
    @Override public void serviceFinished(QuestServiceHub.Kind kind,
            QuestServiceHub.Outcome outcome,String proof){
        mailbox.compareAndSet(null,new Result(kind,outcome,proof));
    }
    /** Consume on a later quest tick and re-observe quest prerequisites before resuming. */
    public Result poll(){
        Result result=mailbox.getAndSet(null);
        if(result!=null)delivered=result;
        return result;
    }
    /** Completion/unsupported results require explicit quest re-planning; HOLD cannot resume. */
    public boolean acknowledge(){
        if(!pending || delivered==null || delivered.kind()!=request.kind()
            || delivered.outcome()==QuestServiceHub.Outcome.HOLD)return false;
        pending=false;request=null;delivered=null;return true;
    }
    public boolean blocksQuestInput(){return pending || QuestServiceHub.mustYield();}
    public void cancel(String reason){QuestServiceHub.cancel(this,reason);}
    public String registryId(){return QuestServiceHub.registryId();}
}
