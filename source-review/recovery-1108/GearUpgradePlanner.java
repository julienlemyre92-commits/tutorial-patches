package net.runelite.client.plugins.microbot.questcommon.preparation.gear;

import java.util.*;

/** Pure, bank-stock-only equipment decisions. The host supplies verified item metadata. */
public final class GearUpgradePlanner {
    public record Item(int id, String slot, boolean membersOnly, boolean twoHanded,
                       Map<String,Integer> requiredLevels, Map<String,Integer> bonuses) {
        public Item {
            if (id <= 0 || slot == null || slot.isBlank()) throw new IllegalArgumentException("item identity");
            requiredLevels = Map.copyOf(requiredLevels);
            bonuses = Map.copyOf(bonuses);
        }
    }
    public record Goal(String accountKey, long deadlineMillis, Map<Integer,Integer> retain,
                       Map<String,Integer> bonusWeights, int minimumFreeSlots, boolean f2pOnly) {
        public Goal {
            if (accountKey == null || accountKey.isBlank() || deadlineMillis <= 0
                || minimumFreeSlots < 0 || minimumFreeSlots > 28) throw new IllegalArgumentException("goal");
            retain = Map.copyOf(retain);
            bonusWeights = Map.copyOf(bonusWeights);
            for (var e : retain.entrySet()) if (e.getKey() <= 0 || e.getValue() < 0)
                throw new IllegalArgumentException("retained item");
            for (int weight : bonusWeights.values()) if (weight < 0)
                throw new IllegalArgumentException("negative bonus weight");
        }
    }
    public record Snapshot(long at, String accountKey, boolean loggedIn, boolean membersWorld,
                           boolean inCombat, boolean bankOpen, boolean bankAudited,
                           boolean equipmentObserved, boolean skillsObserved, int freeSlots,
                           Map<Integer,Integer> inventory, Map<Integer,Integer> bank,
                           Map<String,Integer> equipped, Map<String,Integer> levels,
                           Map<Integer,Item> verifiedItems) {
        public Snapshot {
            inventory = Map.copyOf(inventory); bank = Map.copyOf(bank);
            equipped = Map.copyOf(equipped); levels = Map.copyOf(levels);
            verifiedItems = Map.copyOf(verifiedItems);
        }
        public int carried(int id) { return inventory.getOrDefault(id, 0); }
        public int stored(int id) { return bank.getOrDefault(id, 0); }
    }
    public enum Action { COMPLETE, OPEN_BANK, WITHDRAW_ONE, CLOSE_BANK, EQUIP_ONE, WAIT, HOLD }
    public record Decision(Action action, int itemId, String slot, String reason) { }
    public record PendingAction(Action action, String accountKey, int itemId, String slot,
                                int inventoryBefore, int bankBefore, int equippedBefore,
                                long intentAt) {
        public PendingAction {
            if ((action != Action.WITHDRAW_ONE && action != Action.EQUIP_ONE)
                || accountKey == null || accountKey.isBlank() || itemId <= 0
                || slot == null || slot.isBlank() || intentAt <= 0)
                throw new IllegalArgumentException("pending action");
        }
    }
    public record Checkpoint(String accountKey, PendingAction pending, Set<Integer> attempted, boolean finished) {
        public Checkpoint {
            if (accountKey == null) throw new IllegalArgumentException("checkpoint account");
            if (pending != null && !pending.accountKey().equals(accountKey))
                throw new IllegalArgumentException("pending account mismatch");
            attempted = Set.copyOf(attempted);
        }
        public Properties toProperties() {
            Properties p = new Properties(); p.setProperty("schema", "1");
            p.setProperty("account", accountKey);
            p.setProperty("finished", Boolean.toString(finished));
            p.setProperty("attempted", attempted.stream().sorted().map(String::valueOf)
                .reduce((a,b) -> a + "," + b).orElse(""));
            if (pending != null) {
                p.setProperty("action", pending.action().name());
                p.setProperty("item", Integer.toString(pending.itemId()));
                p.setProperty("slot", pending.slot());
                p.setProperty("inventoryBefore", Integer.toString(pending.inventoryBefore()));
                p.setProperty("bankBefore", Integer.toString(pending.bankBefore()));
                p.setProperty("equippedBefore", Integer.toString(pending.equippedBefore()));
                p.setProperty("intentAt", Long.toString(pending.intentAt()));
            }
            return p;
        }
        public static Checkpoint fromProperties(Properties p) {
            if (!"1".equals(p.getProperty("schema"))) throw new IllegalArgumentException("checkpoint schema");
            Set<Integer> attempted = new HashSet<>();
            String ids = p.getProperty("attempted", "");
            if (!ids.isBlank()) for (String id : ids.split(",")) attempted.add(Integer.parseInt(id));
            PendingAction pending = null;
            if (p.containsKey("action")) pending = new PendingAction(
                Action.valueOf(p.getProperty("action")), p.getProperty("account"),
                Integer.parseInt(p.getProperty("item")), p.getProperty("slot"),
                Integer.parseInt(p.getProperty("inventoryBefore")),
                Integer.parseInt(p.getProperty("bankBefore")),
                Integer.parseInt(p.getProperty("equippedBefore")),
                Long.parseLong(p.getProperty("intentAt")));
            return new Checkpoint(p.getProperty("account"), pending, attempted,
                Boolean.parseBoolean(p.getProperty("finished")));
        }
    }

    private PendingAction pending;
    private final Set<Integer> attempted = new HashSet<>();
    private boolean finished;
    private String boundAccount = "";

    public GearUpgradePlanner() { }
    public GearUpgradePlanner(Checkpoint checkpoint) {
        boundAccount = checkpoint.accountKey(); pending = checkpoint.pending();
        attempted.addAll(checkpoint.attempted()); finished = checkpoint.finished();
    }
    public Checkpoint checkpoint() { return new Checkpoint(boundAccount, pending, attempted, finished); }

    public Decision choose(Goal goal, Snapshot frame) {
        if (!fresh(goal, frame)) return hold("fresh same-account snapshot required");
        if (!bind(goal.accountKey())) return hold("gear pass belongs to another account");
        if (frame.inCombat()) return hold("combat active");
        if (System.currentTimeMillis() > goal.deadlineMillis()) return hold("gear deadline exceeded");
        if (!frame.equipmentObserved() || !frame.skillsObserved())
            return hold("equipment or real skill levels unobserved");
        if (!retained(goal, frame)) return hold("required supplies or emergency coins missing");
        if (pending != null) return goal.accountKey().equals(pending.accountKey())
            ? reconcile(frame) : hold("pending action belongs to another account");
        if (finished) return new Decision(Action.COMPLETE, 0, "", "bank-only upgrade pass finished");
        if (!frame.bankOpen()) return new Decision(Action.OPEN_BANK, 0, "", "audit banked gear");
        if (!frame.bankAudited()) return hold("bank contents not audited");

        Item best = null;
        int bestGain = 0;
        for (Item item : frame.verifiedItems().values()) {
            if (attempted.contains(item.id()) || item.twoHanded() ||
                (item.membersOnly() && (goal.f2pOnly() || !frame.membersWorld())) ||
                frame.carried(item.id()) + frame.stored(item.id()) < 1 ||
                !requirementsMet(item, frame)) continue;
            int oldId = frame.equipped().getOrDefault(item.slot(), 0);
            if (oldId == item.id()) continue;
            Item old = oldId == 0 ? null : frame.verifiedItems().get(oldId);
            if (oldId != 0 && (old == null || !old.slot().equals(item.slot()))) continue;
            int gain = gain(item, old, goal.bonusWeights());
            if (gain > bestGain || (gain == bestGain && gain > 0 && best != null && item.id() < best.id())) {
                best = item; bestGain = gain;
            }
        }
        if (best == null) {
            finished = true;
            return new Decision(Action.COMPLETE, 0, "", "no verified positive banked upgrade");
        }
        if (frame.carried(best.id()) > 0)
            return new Decision(Action.CLOSE_BANK, best.id(), best.slot(), "equip carried upgrade after closing bank");
        if (frame.freeSlots() <= goal.minimumFreeSlots())
            return hold("no free slot beyond protected inventory reserve");
        return new Decision(Action.WITHDRAW_ONE, best.id(), best.slot(), "one banked item; zero spend");
    }

    /** Persist returned intent before sending game input; an uncertain result must never be retried. */
    public Checkpoint beginDispatch(Goal goal, Decision decision, Snapshot before) {
        if (pending != null || decision == null || before == null) throw new IllegalStateException("pending action");
        Decision expected = switch (decision.action()) {
            case WITHDRAW_ONE -> choose(goal, before);
            case EQUIP_ONE -> afterBankClosed(goal, before, decision.itemId());
            default -> throw new IllegalArgumentException("not a gear action");
        };
        if (expected.action() != decision.action() || expected.itemId() != decision.itemId()
            || !expected.slot().equals(decision.slot())) throw new IllegalArgumentException("stale decision");
        pending = new PendingAction(decision.action(), goal.accountKey(), decision.itemId(), decision.slot(),
            before.carried(decision.itemId()), before.stored(decision.itemId()),
            before.equipped().getOrDefault(decision.slot(), 0), System.currentTimeMillis());
        return checkpoint();
    }

    /** Use after closing the bank; the next fresh frame must still prove the carried item. */
    public Decision afterBankClosed(Goal goal, Snapshot frame, int candidateId) {
        if (!fresh(goal, frame) || frame.bankOpen() || !retained(goal, frame))
            return hold("bank closure or supplies not proved");
        if (!bind(goal.accountKey())) return hold("gear pass belongs to another account");
        if (pending != null || finished) return hold("pending or finished gear pass");
        if (frame.inCombat()) return hold("combat active");
        if (System.currentTimeMillis() > goal.deadlineMillis()) return hold("gear deadline exceeded");
        if (!frame.equipmentObserved() || !frame.skillsObserved())
            return hold("equipment or real skill levels unobserved");
        Item item = frame.verifiedItems().get(candidateId);
        if (item == null || frame.carried(candidateId) < 1 || attempted.contains(candidateId)
            || item.twoHanded() || item.membersOnly() && (goal.f2pOnly() || !frame.membersWorld())
            || !requirementsMet(item, frame)) return hold("carried equip candidate not proved");
        int oldId = frame.equipped().getOrDefault(item.slot(), 0);
        Item old = oldId == 0 ? null : frame.verifiedItems().get(oldId);
        if (oldId == candidateId || oldId != 0 && (old == null || !old.slot().equals(item.slot()))
            || gain(item, old, goal.bonusWeights()) <= 0)
            return hold("candidate no longer a verified positive upgrade");
        return new Decision(Action.EQUIP_ONE, candidateId, item.slot(), "equip once; verify next frame");
    }

    private Decision reconcile(Snapshot frame) {
        PendingAction p = pending;
        if (frame.at() <= p.intentAt()) return new Decision(Action.WAIT, p.itemId(), p.slot(), "await newer frame");
        if (p.action() == Action.WITHDRAW_ONE && (!frame.bankOpen() || !frame.bankAudited()))
            return hold("fresh open-bank withdrawal proof unavailable");
        boolean proved = p.action() == Action.WITHDRAW_ONE
            ? frame.carried(p.itemId()) >= p.inventoryBefore() + 1 && frame.stored(p.itemId()) <= p.bankBefore() - 1
            : frame.equipped().getOrDefault(p.slot(), 0) == p.itemId()
              && frame.carried(p.itemId()) < p.inventoryBefore();
        if (!proved) {
            if (System.currentTimeMillis() - p.intentAt() > 4000)
                return hold("action unproved after deadline; stop without repetition");
            return new Decision(Action.WAIT, p.itemId(), p.slot(), "await withdrawal/equip delta");
        }
        pending = null;
        if (p.action() == Action.WITHDRAW_ONE)
            return new Decision(Action.CLOSE_BANK, p.itemId(), p.slot(), "withdrawal proved; close bank");
        attempted.add(p.itemId());
        return new Decision(Action.OPEN_BANK, p.itemId(), p.slot(), "equip proved; re-audit for other slots");
    }

    private static int gain(Item item, Item old, Map<String,Integer> weights) {
        long score = 0;
        for (var e : weights.entrySet()) {
            int delta = item.bonuses().getOrDefault(e.getKey(), 0)
                - (old == null ? 0 : old.bonuses().getOrDefault(e.getKey(), 0));
            score += (long) e.getValue() * delta;
        }
        return score > Integer.MAX_VALUE ? Integer.MAX_VALUE : score < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) score;
    }
    private static boolean requirementsMet(Item item, Snapshot frame) {
        for (var e : item.requiredLevels().entrySet())
            if (frame.levels().getOrDefault(e.getKey(), -1) < e.getValue()) return false;
        return true;
    }
    private static boolean retained(Goal goal, Snapshot frame) {
        for (var e : goal.retain().entrySet())
            if (frame.carried(e.getKey()) < e.getValue()) return false;
        return true;
    }
    private static boolean fresh(Goal goal, Snapshot frame) {
        return frame != null && frame.loggedIn() && goal.accountKey().equals(frame.accountKey())
            && frame.at() > 0 && frame.at() <= System.currentTimeMillis()
            && System.currentTimeMillis() - frame.at() <= 3000;
    }
    private boolean bind(String accountKey) {
        if (boundAccount.isEmpty()) { boundAccount = accountKey; return true; }
        return boundAccount.equals(accountKey);
    }
    private static Decision hold(String reason) { return new Decision(Action.HOLD, 0, "", reason); }
}
