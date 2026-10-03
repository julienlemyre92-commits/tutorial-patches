package net.runelite.client.plugins.microbot.util.walker;

import java.util.concurrent.atomic.AtomicLong;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

/** Bounded run activation shared by every route using Rs2Walker. */
public final class RunTogglePolicy {
    private static final AtomicLong LAST_ATTEMPT_MS = new AtomicLong();
    private RunTogglePolicy() { }

    public static boolean toggleIfDue(boolean enable) {
        if (!enable || Rs2Player.isRunEnabled() || Rs2Player.getRunEnergy() < 20)
            return false;
        long now = System.currentTimeMillis();
        long previous = LAST_ATTEMPT_MS.get();
        if (now - previous < 5_000L || !LAST_ATTEMPT_MS.compareAndSet(previous, now))
            return false;
        return Rs2Player.toggleRunEnergy(true);
    }
}
