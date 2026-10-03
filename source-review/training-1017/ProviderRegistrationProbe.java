import java.util.EnumMap;
import java.util.Map;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;

/** Offline check of the five-provider atomic first-install contract. */
public final class ProviderRegistrationProbe {
    public static void main(String[] args) {
        String token = QuestServiceHub.beginReload();
        if (token == null) throw new IllegalStateException("Reload fence rejected");
        Map<QuestServiceHub.Kind, QuestServiceHub.ServicePlugin> providers =
            new EnumMap<>(QuestServiceHub.Kind.class);
        for (QuestServiceHub.Kind kind : new QuestServiceHub.Kind[] {
                QuestServiceHub.Kind.NAVIGATION, QuestServiceHub.Kind.MONEY_MAKING,
                QuestServiceHub.Kind.FOOD_RESTOCK, QuestServiceHub.Kind.BANKING,
                QuestServiceHub.Kind.TRAINING }) {
            providers.put(kind, () -> kind);
        }
        if (!QuestServiceHub.installProviders(token, providers))
            throw new IllegalStateException("Five-provider registration rejected");
        if (!QuestServiceHub.endReload(token))
            throw new IllegalStateException("Reload fence did not drain");
        for (QuestServiceHub.Kind kind : providers.keySet()) {
            if (!QuestServiceHub.isRegistered(kind))
                throw new IllegalStateException("Missing provider " + kind);
        }
        System.out.println("PASS five-provider atomic registration");
    }
}
