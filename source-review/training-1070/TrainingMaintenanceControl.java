package net.runelite.client.plugins.microbot.questcommon.training;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Provider child adapter. The walker-idle proof comes from the already installed NAV provider. */
public final class TrainingMaintenanceControl implements CombatTrainingService.MaintenancePort {
    private final Path hotHome;
    private final Path checkpointHome;
    private final BooleanSupplier walkerIdle;

    public TrainingMaintenanceControl(Path hotHome, Path checkpointHome, BooleanSupplier walkerIdle) {
        this.hotHome = Objects.requireNonNull(hotHome).toAbsolutePath();
        this.checkpointHome = Objects.requireNonNull(checkpointHome).toAbsolutePath();
        this.walkerIdle = Objects.requireNonNull(walkerIdle);
    }

    @Override public TrainingMaintenanceProtocol.Probe inspect(
        CombatTrainingGoal goal, CombatTrainingService.Frame frame, long now) {
        return TrainingMaintenanceProtocol.inspect(hotHome,
            ProcessHandle.current().pid(), frame.accountKey(), goal.requestId(), now);
    }

    @Override public TrainingMaintenanceProtocol.Receipt checkpoint(
        CombatTrainingGoal goal, CombatTrainingService.Frame f,
        TrainingMaintenanceProtocol.Update update, long now) throws Exception {
        if (f == null || f.position() == null || f.accountKey() == null
            || !goal.accountKey().equals(f.accountKey())
            || !walkerIdle()) throw new IllegalStateException("fresh identity/walker-idle proof absent");
        return TrainingMaintenanceProtocol.checkpoint(checkpointHome, update,
            f.position().getX(), f.position().getY(), f.position().getPlane(), f.world(),
            f.combatLevel(), f.hpLevel(), f.hp(), f.maxHp(), f.attackXp(), f.strengthXp(),
            f.defenceXp(), f.rangedXp(), f.magicXp(), f.hpXp(), f.inventory(), f.equipped(), now);
    }

    @Override public boolean walkerIdle() {
        try { return walkerIdle.getAsBoolean(); }
        catch (RuntimeException | LinkageError failure) { return false; }
    }
}
