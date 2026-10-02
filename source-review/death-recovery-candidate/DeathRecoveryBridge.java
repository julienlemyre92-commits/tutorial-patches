package net.runelite.client.plugins.microbot.questcommon.deathplugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import net.runelite.api.coords.WorldPoint;

/** Parent-classloader contract between one quest owner and the recovery plugin. */
public final class DeathRecoveryBridge {
    public enum Outcome { RECOVERED, NOTHING_TO_RECLAIM, STOPPED_SAFE, STOPPED_UNSAFE }

    public interface Owner {
        /** Stop quest clicks and cancel/join any in-flight walker before returning true. */
        void requestYield();
        boolean isQuiescent();
        /** Return a fresh, UI-proved total cost; empty means fee unknown. */
        OptionalInt officeFeeQuote();
        /** Called once. A STOPPED outcome must leave the quest paused. */
        void recoveryFinished(Outcome outcome, String detail);
    }

    public static final class Policy {
        public final Map<Integer, Integer> criticalPossession;
        public final WorldPoint safeExit;
        public final Predicate<WorldPoint> safeGrave;
        public final int feeCap;
        public Policy(Map<Integer, Integer> criticalPossession, WorldPoint safeExit,
                      Predicate<WorldPoint> safeGrave, int feeCap) {
            Objects.requireNonNull(criticalPossession);
            if (criticalPossession.values().stream().anyMatch(v -> v == null || v < 1)
                || criticalPossession.keySet().stream().anyMatch(id -> id == null || id < 1)
                || safeExit == null || safeGrave == null || feeCap < 0)
                throw new IllegalArgumentException("Invalid death recovery policy");
            this.criticalPossession = Collections.unmodifiableMap(new HashMap<>(criticalPossession));
            this.safeExit = safeExit;
            this.safeGrave = safeGrave;
            this.feeCap = feeCap;
        }
    }

    public static final class Registration {
        public final Owner owner;
        public final Policy policy;
        private Registration(Owner owner, Policy policy) {
            this.owner = owner; this.policy = policy;
        }
    }

    private static final AtomicReference<Registration> REGISTERED = new AtomicReference<>();
    private static final AtomicBoolean YIELD = new AtomicBoolean();
    private DeathRecoveryBridge() { }

    public static synchronized boolean register(Owner owner, Policy policy) {
        return REGISTERED.compareAndSet(null,
            new Registration(Objects.requireNonNull(owner), Objects.requireNonNull(policy)));
    }
    public static synchronized boolean unregister(Owner owner) {
        Registration current = REGISTERED.get();
        return current != null && current.owner == owner && !YIELD.get()
            && REGISTERED.compareAndSet(current, null);
    }
    public static boolean mustYield() { return YIELD.get(); }
    static Registration registration() { return REGISTERED.get(); }
    static synchronized boolean requestYield(Registration expected) {
        if (REGISTERED.get() != expected || !YIELD.compareAndSet(false, true)) return false;
        expected.owner.requestYield();
        return true;
    }
    static void finish(Registration expected, Outcome outcome, String detail, boolean release) {
        try { expected.owner.recoveryFinished(outcome, detail); }
        finally { if (release && REGISTERED.get() == expected) YIELD.set(false); }
    }
}
