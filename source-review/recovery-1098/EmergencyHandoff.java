package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.util.function.BooleanSupplier;
import net.runelite.client.plugins.microbot.questcommon.navigation.escape.EmergencyLocalEscape;

/** Candidate-only extension; QuestServiceHub and NavigationService.Driver ABI stay unchanged. */
public interface EmergencyHandoff {
    boolean ordinaryQuiescent();
    void beginEmergency(NavigationGoal goal, BooleanSupplier ownsInput);
    EmergencyLocalEscape.Result tickEmergency();
    boolean emergencyQuiescent();
    void cancelEmergency();
    /** Forget a finished escape only after every worker and guard has stopped. */
    void releaseEmergency();
}
