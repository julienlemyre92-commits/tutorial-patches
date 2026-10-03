import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Replace only Rs2Walker's run-orb call; preserve all custom route bytecode. */
public final class PatchWalkerRunToggle {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input.class output.class");
        byte[] original = Files.readAllBytes(Path.of(args[0]));
        ClassNode klass = new ClassNode();
        new ClassReader(original).accept(klass, 0);
        if (!"net/runelite/client/plugins/microbot/util/walker/Rs2Walker".equals(klass.name))
            throw new IllegalArgumentException("Wrong class");
        int matches = 0;
        for (MethodNode method : klass.methods) {
            if (!"manageRunEnergy".equals(method.name) || !"(I)V".equals(method.desc)) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode call)) continue;
                if (call.getOpcode() != Opcodes.INVOKESTATIC
                    || !"net/runelite/client/plugins/microbot/util/player/Rs2Player".equals(call.owner)
                    || !"toggleRunEnergy".equals(call.name) || !"(Z)Z".equals(call.desc)) continue;
                call.owner = "net/runelite/client/plugins/microbot/util/walker/RunTogglePolicy";
                call.name = "toggleIfDue";
                matches++;
            }
        }
        if (matches != 1) throw new IllegalStateException("Expected one run toggle, got " + matches);
        ClassWriter writer = new ClassWriter(0);
        klass.accept(writer);
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
