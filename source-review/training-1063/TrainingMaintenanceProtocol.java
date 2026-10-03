package net.runelite.client.plugins.microbot.questcommon.training;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Value-only file protocol. This source is compiled separately into provider and quest artifacts. */
public final class TrainingMaintenanceProtocol {
    public static final String PROOF_PREFIX = "TRAINING_MAINTENANCE_YIELD_V1";
    private static final long STATUS_MAX_AGE_MS = 10_000;
    private static final long WAIT_MAX_MS = 120_000;
    private TrainingMaintenanceProtocol() { }

    public enum ProbeState { NONE, READY, INVALID }
    public enum WaitState { WAITING, APPLIED, FAILED }
    public record Update(long pid, String accountKey, String requestId,
        int fromGeneration, String fromArtifact, int generation, String artifact,
        String manifest, String marker, String parentAbi, long requestedAt) { }
    public record Probe(ProbeState state, Update update, String reason) { }
    public record Receipt(Update update, String checkpointName, String checkpointSha,
        long createdAt) { }
    public record Wait(WaitState state, String reason) { }

    /** Observation time is not part of immutable update identity. */
    public static boolean sameRequest(Update a, Update b) {
        return a != null && b != null && a.pid() == b.pid()
            && a.accountKey().equals(b.accountKey()) && a.requestId().equals(b.requestId())
            && a.fromGeneration() == b.fromGeneration() && a.fromArtifact().equals(b.fromArtifact())
            && a.generation() == b.generation() && a.artifact().equals(b.artifact())
            && a.manifest().equals(b.manifest()) && a.marker().equals(b.marker())
            && a.parentAbi().equals(b.parentAbi());
    }

    /**
     * Returns NONE when no newer update is requested. A present but malformed/stale request is
     * INVALID so the training provider can stop issuing new attacks and fail closed at a safe point.
     */
    public static Probe inspect(Path hotHome, long pid, String accountKey, String requestId, long now) {
        if (hotHome == null || pid <= 0 || !hash(accountKey) || !safeId(requestId))
            return invalid("invalid caller identity");
        Path requestFile = hotHome.resolve("request.properties");
        if (!Files.exists(requestFile)) return new Probe(ProbeState.NONE, null, "no request");
        try {
            Properties req = load(requestFile);
            Properties status = load(hotHome.resolve("status.properties"));
            long statusPid = number(status, "pid");
            long statusAt = number(status, "timestamp");
            int from = Math.toIntExact(number(status, "generation"));
            String phase = status.getProperty("phase", "");
            String fromArtifact = status.getProperty("artifactSha", "");
            String oldMarker = status.getProperty("marker", "");
            String parentAbi = status.getProperty("parentAbi", "");
            int generation = Integer.parseInt(req.getProperty("generation", "0"));
            String artifact = req.getProperty("artifactSha256", "");
            String manifest = req.getProperty("manifestSha256", "");
            if (statusPid != pid || !"APPLIED".equals(phase) && !"DEFERRED".equals(phase)
                || statusAt <= 0 || now < statusAt || now - statusAt > STATUS_MAX_AGE_MS
                || !hash(fromArtifact) || !hash(oldMarker) || !hash(parentAbi))
                return invalid("provider status is stale, failed, or belongs to another client");
            if (!hash(artifact) || !hash(manifest) || generation <= 0)
                return invalid("request hashes/generation malformed");
            if (generation <= from) {
                if (generation == from && artifact.equals(fromArtifact))
                    return new Probe(ProbeState.NONE, null, "requested generation already loaded");
                return generation < from ? new Probe(ProbeState.NONE, null, "request older than loaded provider")
                    : invalid("same generation names a different provider artifact");
            }
            Path jar = hotHome.resolve(artifact + ".jar");
            Path manifestFile = hotHome.resolve(manifest + ".properties");
            byte[] manifestBytes = Files.readAllBytes(manifestFile);
            if (!manifest.equals(sha(manifestBytes)) || !artifact.equals(sha(Files.readAllBytes(jar))))
                return invalid("immutable artifact/manifest digest mismatch");
            Properties descriptor = load(manifestFile);
            String marker = descriptor.getProperty("implementationMarker", "");
            String requestedAbi = descriptor.getProperty("parentAbiSha256", "");
            if (!"1".equals(descriptor.getProperty("format"))
                || !Integer.toString(generation).equals(descriptor.getProperty("generation"))
                || !artifact.equals(descriptor.getProperty("artifactSha256"))
                || !parentAbi.equals(requestedAbi) || !hash(marker))
                return invalid("request manifest does not match loaded parent ABI or artifact");
            Update update = new Update(pid, accountKey, requestId, from, fromArtifact,
                generation, artifact, manifest, marker, parentAbi, now);
            return new Probe(ProbeState.READY, update, "newer verified provider request");
        } catch (Exception failure) {
            return invalid("cannot verify provider request: " + failure.getClass().getSimpleName());
        }
    }

    /** Persist a value-only same-process/account/request checkpoint before releasing the lease. */
    public static Receipt checkpoint(Path root, Update update, int x, int y, int plane, int world,
        int combat, int hpLevel, int hp, int maxHp, long attackXp, long strengthXp,
        long defenceXp, long rangedXp, long magicXp, long hpXp,
        Map<Integer,Integer> inventory, Set<Integer> equipment, long now) throws IOException {
        Objects.requireNonNull(root); Objects.requireNonNull(update);
        if (x < 0 || y < 0 || plane < 0 || plane > 3 || world <= 0 || combat < 1
            || hpLevel < 10 || hp < 1 || maxHp < 1 || now < update.requestedAt())
            throw new IOException("invalid fresh training checkpoint frame");
        Files.createDirectories(root);
        String name = "training-" + update.pid() + "-" + update.accountKey() + "-"
            + update.requestId() + "-g" + update.generation() + ".properties";
        Path target = root.resolve(name);
        Properties p = new Properties();
        p.setProperty("schema", "1"); p.setProperty("pid", Long.toString(update.pid()));
        p.setProperty("accountKey", update.accountKey()); p.setProperty("requestId", update.requestId());
        p.setProperty("fromGeneration", Integer.toString(update.fromGeneration()));
        p.setProperty("fromArtifact", update.fromArtifact());
        p.setProperty("generation", Integer.toString(update.generation()));
        p.setProperty("artifact", update.artifact()); p.setProperty("manifest", update.manifest());
        p.setProperty("marker", update.marker()); p.setProperty("parentAbi", update.parentAbi());
        p.setProperty("createdAt", Long.toString(now)); p.setProperty("world", Integer.toString(world));
        p.setProperty("x", Integer.toString(x)); p.setProperty("y", Integer.toString(y));
        p.setProperty("plane", Integer.toString(plane)); p.setProperty("combat", Integer.toString(combat));
        p.setProperty("hpLevel", Integer.toString(hpLevel)); p.setProperty("hp", Integer.toString(hp));
        p.setProperty("maxHp", Integer.toString(maxHp)); p.setProperty("attackXp", Long.toString(attackXp));
        p.setProperty("strengthXp", Long.toString(strengthXp)); p.setProperty("defenceXp", Long.toString(defenceXp));
        p.setProperty("rangedXp", Long.toString(rangedXp)); p.setProperty("magicXp", Long.toString(magicXp));
        p.setProperty("hpXp", Long.toString(hpXp)); p.setProperty("inventory", encodeInventory(inventory));
        p.setProperty("equipment", encodeSet(equipment));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        p.store(bytes, "Training maintenance checkpoint; no live game objects");
        byte[] data = bytes.toByteArray();
        if (Files.exists(target)) {
            byte[] existing = Files.readAllBytes(target);
            if (!sha(existing).equals(sha(data))) throw new IOException("checkpoint path already occupied");
            return new Receipt(update, name, sha(existing), now);
        }
        Path temp = Files.createTempFile(root, ".training-maintenance-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
                    while (buffer.hasRemaining()) channel.write(buffer);
                    channel.force(true);
            }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) {
                throw new IOException("atomic checkpoint commit unsupported; do not yield", unsupported);
            }
        } finally { Files.deleteIfExists(temp); }
        return new Receipt(update, name, sha(data), now);
    }

    public static String proof(Receipt r) {
        Update u = r.update();
        return PROOF_PREFIX + "|" + u.pid() + "|" + u.accountKey() + "|" + u.requestId()
            + "|" + u.fromGeneration() + "|" + u.fromArtifact() + "|" + u.generation()
            + "|" + u.artifact() + "|" + u.manifest() + "|" + u.marker()
            + "|" + u.parentAbi() + "|" + r.checkpointName() + "|" + r.checkpointSha()
            + "|" + r.createdAt();
    }

    public static Receipt parseProof(String proof) {
        try {
            String[] a = proof.split("\\|", -1);
            if (a.length != 14 || !PROOF_PREFIX.equals(a[0])) return null;
            long pid = Long.parseLong(a[1]); String account = a[2], requestId = a[3];
            int from = Integer.parseInt(a[4]); String fromArtifact = a[5];
            int gen = Integer.parseInt(a[6]); String artifact = a[7], manifest = a[8], marker = a[9];
            String parentAbi = a[10], checkpoint = a[11], checkpointSha = a[12];
            long createdAt = Long.parseLong(a[13]);
            if (pid <= 0 || !hash(account) || !safeId(requestId) || from < 0 || gen <= from
                || !hash(fromArtifact) || !hash(artifact) || !hash(manifest) || !hash(marker)
                || !hash(parentAbi)
                || !checkpoint.matches("training-[0-9]+-[a-f0-9]{64}-[A-Za-z0-9._-]{1,80}-g[0-9]+\\.properties")
                || !hash(checkpointSha) || createdAt <= 0) return null;
            Update u = new Update(pid, account, requestId, from, fromArtifact, gen,
                artifact, manifest, marker, parentAbi, createdAt);
            return new Receipt(u, checkpoint, checkpointSha, createdAt);
        } catch (Exception failure) { return null; }
    }

    /** Quest-side gate: only exact proof + immutable request/checkpoint + exact applied status resumes. */
    public static Wait await(Path hotHome, Path checkpointRoot, Receipt receipt,
        long pid, String accountKey, String requestId, long now) {
        if (receipt == null || receipt.update().pid() != pid
            || !receipt.update().accountKey().equals(accountKey)
            || !receipt.update().requestId().equals(requestId))
            return failed("maintenance proof identity mismatch");
        try {
            Update u = receipt.update();
            Properties req = load(hotHome.resolve("request.properties"));
            if (!Integer.toString(u.generation()).equals(req.getProperty("generation"))
                || !u.artifact().equals(req.getProperty("artifactSha256"))
                || !u.manifest().equals(req.getProperty("manifestSha256")))
                return failed("requested provider changed while maintenance was pending");
            Path manifestPath = hotHome.resolve(u.manifest() + ".properties");
            if (!u.manifest().equals(sha(Files.readAllBytes(manifestPath)))
                || !u.artifact().equals(sha(Files.readAllBytes(hotHome.resolve(u.artifact() + ".jar"))))
                || !u.marker().equals(load(manifestPath).getProperty("implementationMarker"))
                || !u.parentAbi().equals(load(manifestPath).getProperty("parentAbiSha256")))
                return failed("requested provider artifact changed after yield");
            Path checkpoint = checkpointRoot.resolve(receipt.checkpointName());
            byte[] checkpointBytes = Files.readAllBytes(checkpoint);
            if (!receipt.checkpointSha().equals(sha(checkpointBytes)))
                return failed("checkpoint digest mismatch");
            Properties cp = load(checkpoint);
            if (!"1".equals(cp.getProperty("schema"))
                || !Long.toString(pid).equals(cp.getProperty("pid"))
                || !accountKey.equals(cp.getProperty("accountKey"))
                || !requestId.equals(cp.getProperty("requestId"))
                || !Integer.toString(u.generation()).equals(cp.getProperty("generation"))
                || !u.artifact().equals(cp.getProperty("artifact"))
                || !u.manifest().equals(cp.getProperty("manifest"))
                || !u.marker().equals(cp.getProperty("marker")))
                return failed("checkpoint identity or request mismatch");
            long created = number(cp, "createdAt");
            if (created != receipt.createdAt() || now < created || now - created > WAIT_MAX_MS)
                return failed("maintenance application timed out or checkpoint time mismatch");
            Properties status = load(hotHome.resolve("status.properties"));
            if (number(status, "pid") != pid) return failed("provider status PID mismatch");
            if (!u.parentAbi().equals(status.getProperty("parentAbi")))
                return failed("provider parent ABI changed during reload");
            long timestamp = number(status, "timestamp");
            if (timestamp <= 0 || timestamp > now || now - timestamp > STATUS_MAX_AGE_MS)
                return new Wait(WaitState.WAITING, "waiting for fresh provider host status");
            String phase = status.getProperty("phase", "");
            if ("FAILED_CLOSED".equals(phase)) return failed("provider host entered FAILED_CLOSED");
            int generation = Math.toIntExact(number(status, "generation"));
            String artifact = status.getProperty("artifactSha", "");
            String marker = status.getProperty("marker", "");
            if (generation == u.generation() && artifact.equals(u.artifact())
                && marker.equals(u.marker()) && "APPLIED".equals(phase))
                return new Wait(WaitState.APPLIED, "exact requested provider marker loaded");
            if (generation > u.generation()) return failed("provider advanced past requested generation");
            if (generation == u.generation()) return failed("generation loaded with wrong artifact or marker");
            return new Wait(WaitState.WAITING, "provider update not yet applied");
        } catch (Exception failure) {
            return failed("maintenance verification failed: " + failure.getClass().getSimpleName());
        }
    }

    public static boolean matches(Receipt r, long pid, String account, String requestId) {
        return r != null && r.update().pid() == pid && r.update().accountKey().equals(account)
            && r.update().requestId().equals(requestId);
    }
    private static Probe invalid(String why) { return new Probe(ProbeState.INVALID, null, why); }
    private static Wait failed(String why) { return new Wait(WaitState.FAILED, why); }
    private static Properties load(Path path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        return p;
    }
    private static long number(Properties p, String key) {
        return Long.parseLong(p.getProperty(key, ""));
    }
    private static boolean hash(String s) { return s != null && s.matches("[a-f0-9]{64}"); }
    private static boolean safeId(String id) { return id != null && id.matches("[A-Za-z0-9._-]{1,80}"); }
    private static String sha(byte[] data) throws IOException {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (Exception failure) { throw new IOException("SHA-256 unavailable", failure); }
    }
    private static String encodeInventory(Map<Integer,Integer> inventory) throws IOException {
        if (inventory == null || inventory.size() > 28) throw new IOException("invalid inventory checkpoint");
        StringJoiner out = new StringJoiner(",");
        inventory.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            if (e.getKey() <= 0 || e.getValue() <= 0) throw new IllegalArgumentException("invalid inventory item");
            out.add(e.getKey() + "=" + e.getValue());
        });
        return out.toString();
    }
    private static String encodeSet(Set<Integer> values) throws IOException {
        if (values == null || values.size() > 14 || values.stream().anyMatch(i -> i == null || i <= 0))
            throw new IOException("invalid equipment checkpoint");
        return values.stream().sorted().map(String::valueOf).reduce((a,b) -> a + "," + b).orElse("");
    }
}
