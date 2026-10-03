package net.runelite.client.plugins.microbot.questcommon.hot.impl;

import java.nio.file.Path;
import java.util.*;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;
import net.runelite.client.plugins.microbot.questcommon.hot.*;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;
import net.runelite.client.plugins.microbot.questcommon.navigation.*;
import net.runelite.client.plugins.microbot.questcommon.funding.*;
import net.runelite.client.plugins.microbot.questcommon.acquisition.*;
import net.runelite.client.plugins.microbot.questcommon.preparation.*;
import net.runelite.client.plugins.microbot.questcommon.training.*;
import net.runelite.client.plugins.microbot.questcommon.ge.QuestGeSellWidgetAdapter;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;

/** One implementation generation. No constructor starts workers or registers providers. */
public final class DefaultProviderBundleFactory implements ProviderBundleFactory {
    private static final String AGENT="MicrobotQuestServices/1 (Julien private-server testing)";
    private static final Path HOME=Path.of(System.getProperty("user.home"),".runelite","quest-services");
    @Override public String implementationMarker(){return ProviderImplBuild.ID;}
    @Override public String parentAbiSha256(){return ProviderImplBuild.PARENT_ABI_SHA256;}
    @Override public String preflightFailure(){
        String guard=InstalledNavigationGuard.discover().installationFailure();
        return guard==null?null:"navigation guard unavailable: "+guard;
    }
    @Override public Map<QuestServiceHub.Kind,ProviderRuntime> createAll() throws Exception {
        EnumMap<QuestServiceHub.Kind,ProviderRuntime> result=new EnumMap<>(QuestServiceHub.Kind.class);
        try {
            InstalledNavigationGuard guard=InstalledNavigationGuard.discover();
            if(guard.installationFailure()!=null)throw new IllegalStateException(preflightFailure());
            NavigationMicrobotDriver driver=new NavigationMicrobotDriver(new VerifiedRoutePolicy(),guard);
            NavigationService navigation=new NavigationService(driver);
            result.put(QuestServiceHub.Kind.NAVIGATION,new Handle(navigation,navigation::tick,
                navigation::stop,()->{},driver::idleForReload));

            FundingMicrobotAdapter.FeeEvidence noAssumedFee=(price,quantity)->OptionalInt.empty();
            FundingService funding=new FundingService(new FundingMicrobotAdapter(account->null,
                noAssumedFee,new QuestGeSellWidgetAdapter.UiProfile("Sell offer"),AGENT),
                HOME.resolve("funding.properties"));
            result.put(QuestServiceHub.Kind.MONEY_MAKING,new Handle(funding,funding::tick,
                ()->{},funding::close,()->QuestServiceHub.activeFor(funding)==null));

            PreparationFoodRouter router=PreparationFoodRouter.shared();
            FoodAcquisitionService food=new FoodAcquisitionService(
                new FoodAcquisitionMicrobotAdapter(router,AGENT),
                ()->QuestServiceHub.isRegistered(QuestServiceHub.Kind.MONEY_MAKING),
                HOME.resolve("food.properties"));
            result.put(QuestServiceHub.Kind.FOOD_RESTOCK,new Handle(food,food::tick,
                ()->{},food::close,()->QuestServiceHub.activeFor(food)==null));

            List<PreparationPlanner.Food> foods=List.of(food(Rs2Food.TROUT),food(Rs2Food.SALMON),
                food(Rs2Food.TUNA),food(Rs2Food.LOBSTER));
            PreparationBankService preparation=new PreparationBankService(
                new PreparationMicrobotAdapter(foods),HOME.resolve("preparation.properties"),router);
            result.put(QuestServiceHub.Kind.BANKING,new Handle(preparation,preparation::tick,
                ()->{},preparation::close,()->QuestServiceHub.activeFor(preparation)==null));

            CombatTrainingMicrobotDriver trainingDriver=new CombatTrainingMicrobotDriver();
            CombatTrainingService training=new CombatTrainingService(trainingDriver);
            result.put(QuestServiceHub.Kind.TRAINING,new Handle(training,training::tick,
                training::stop,()->{},()->QuestServiceHub.activeFor(training)==null
                    && trainingDriver.stopped()));
            return Map.copyOf(result);
        } catch(Exception | Error ex) {
            for(ProviderRuntime runtime:result.values())try{runtime.close();}catch(Exception ignored){}
            throw ex;
        }
    }
    private static PreparationPlanner.Food food(Rs2Food type){
        return new PreparationPlanner.Food(type.getId(),type.getHeal(),false);
    }
    private interface Closer {void close() throws Exception;}
    private record Handle(QuestServiceHub.ServicePlugin service,Runnable ticker,
                          Runnable stopper,Closer closer,BooleanSupplier idler) implements ProviderRuntime {
        @Override public QuestServiceHub.Kind kind(){return service.kind();}
        @Override public void tick(){ticker.run();}
        @Override public boolean idleForReload(){return idler.getAsBoolean();}
        @Override public void requestStop(){stopper.run();}
        @Override public void close() throws Exception {closer.close();}
    }
}
