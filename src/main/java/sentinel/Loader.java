package sentinel;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Sentinel Java Instrumentation Agent.
 * <p>
 * Implements {@link ClassFileTransformer} to dynamically inspect and transform target
 * application bytecode at class-load time via the OW2 ASM 9.7.1 Tree API.
 * <p>
 * Supports three transformation modes (see {@link TransformMode}):
 * <ul>
 *   <li>{@code REPLACE} — replaces the return value with a constant string</li>
 *   <li>{@code TRACE}   — injects entry/exit timing probes</li>
 *   <li>{@code COUNT}   — injects an atomic invocation counter</li>
 * </ul>
 * Configuration is supplied via {@code -javaagent} arguments (see {@link AgentConfig}).
 *
 * @author JOJIN JOHN
 */
public final class Loader implements ClassFileTransformer {

    private static final String TRANSFORMED_MESSAGE = "Transformed message";
    private static final String TARGET_METHOD_DESC  = "()Ljava/lang/String;";

    private final AgentConfig config;

    /**
     * Constructs a new {@code Loader} transformer with the given configuration.
     *
     * @param config the parsed agent configuration
     */
    public Loader(AgentConfig config) {
        this.config = config;
    }

    /**
     * Intercepts and selectively transforms class byte streams during class loading or retransformation.
     *
     * @param loader              the defining loader of the class to be transformed
     * @param className           the internal JVM name of the class (slash-separated)
     * @param classBeingRedefined the class being redefined, or {@code null}
     * @param protectionDomain    the protection domain of the class
     * @param classfileBuffer     the input byte buffer in class file format
     * @return the transformed classfile buffer, or {@code null} if no transformation was performed
     */
    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        // 1. Null-safety guards
        if (className == null || classfileBuffer == null || classfileBuffer.length == 0) {
            return null;
        }

        AgentStats.recordScan();

        // 2. Strict scope gating — only process the configured target class
        if (!config.getTargetClass().equals(className)) {
            return null;
        }

        if (config.isVerbose()) {
            System.out.println("[Sentinel] Inspecting: " + className.replace('/', '.'));
        }

        try {
            byte[] result = switch (config.getMode()) {
                case REPLACE -> applyReplace(classfileBuffer);
                case TRACE   -> applyTrace(classfileBuffer);
                case COUNT   -> applyCount(classfileBuffer);
            };

            if (result != null) {
                AgentStats.recordTransform();
                System.out.println("[Sentinel] Transformed: " + className.replace('/', '.')
                        + " [mode=" + config.getMode() + "]");
            }
            return result;

        } catch (Throwable t) {
            AgentStats.recordError();
            System.err.println("[Sentinel] Error transforming " + className + ": " + t.getMessage());
            t.printStackTrace(System.err);
            return null;
        }
    }

    // ─── REPLACE mode ────────────────────────────────────────────────────────

    /**
     * Replaces the return value of the target method with a constant {@code "Transformed message"}.
     *
     * @param classfileBuffer original bytecode
     * @return transformed bytecode, or {@code null} if no change
     */
    private byte[] applyReplace(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_FRAMES);

        boolean modified = false;

        for (MethodNode method : classNode.methods) {
            if (config.getTargetMethod().equals(method.name)
                    && TARGET_METHOD_DESC.equals(method.desc)) {

                if (isAlreadyReplaced(method)) {
                    if (config.isVerbose()) {
                        System.out.println("[Sentinel] Already replaced; skipping.");
                    }
                    return null;
                }

                InsnList newInstructions = new InsnList();
                newInstructions.add(new LdcInsnNode(TRANSFORMED_MESSAGE));
                newInstructions.add(new InsnNode(Opcodes.ARETURN));
                method.instructions.clear();
                method.instructions.add(newInstructions);
                modified = true;
                break;
            }
        }

        if (!modified) {
            return null;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isAlreadyReplaced(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LdcInsnNode ldc && TRANSFORMED_MESSAGE.equals(ldc.cst)) {
                return true;
            }
        }
        return false;
    }

    // ─── TRACE mode ──────────────────────────────────────────────────────────

    /**
     * Injects entry and exit timing probes around the target method using {@code System.nanoTime()}.
     *
     * @param classfileBuffer original bytecode
     * @return transformed bytecode, or {@code null} if no change
     */
    private byte[] applyTrace(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        String targetMethod = config.getTargetMethod();

        reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (targetMethod.equals(name)) {
                    return new TraceMethodAdapter(Opcodes.ASM9, mv, access, name, descriptor);
                }
                return mv;
            }
        }, ClassReader.SKIP_FRAMES);

        return writer.toByteArray();
    }

    /** ASM {@link AdviceAdapter} that injects entry/exit timing probes. */
    private static final class TraceMethodAdapter extends AdviceAdapter {

        private final String methodName;
        private int startTimeVar;

        TraceMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
            this.methodName = name;
        }

        @Override
        protected void onMethodEnter() {
            // long startTime = System.nanoTime();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            startTimeVar = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, startTimeVar);

            // System.out.println("[Sentinel] ENTER " + methodName);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("[Sentinel] ENTER " + methodName + "()");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == Opcodes.ATHROW) {
                return; // don't probe exceptional exits
            }
            // long elapsed = System.nanoTime() - startTime;
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            mv.visitVarInsn(Opcodes.LLOAD, startTimeVar);
            mv.visitInsn(Opcodes.LSUB);

            // convert ns -> ms: elapsed / 1_000_000
            mv.visitLdcInsn(1_000_000L);
            mv.visitInsn(Opcodes.LDIV);

            // store ms result
            int msVar = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, msVar);

            // System.out.println("[Sentinel] EXIT methodName() — took " + ms + "ms");
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("[Sentinel] EXIT  " + methodName + "() — took ");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(Opcodes.LLOAD, msVar);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(J)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn("ms");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
        }
    }

    // ─── COUNT mode ──────────────────────────────────────────────────────────

    /**
     * Injects an {@link AgentStats#recordInvocation()} call at the entry of the target method,
     * then prints the running invocation count.
     *
     * @param classfileBuffer original bytecode
     * @return transformed bytecode, or {@code null} if no change
     */
    private byte[] applyCount(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        String targetMethod = config.getTargetMethod();

        reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (targetMethod.equals(name)) {
                    return new CountMethodAdapter(Opcodes.ASM9, mv, access, name, descriptor);
                }
                return mv;
            }
        }, ClassReader.SKIP_FRAMES);

        return writer.toByteArray();
    }

    /** ASM {@link AdviceAdapter} that increments and prints an invocation counter. */
    private static final class CountMethodAdapter extends AdviceAdapter {

        private final String methodName;

        CountMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
            this.methodName = name;
        }

        @Override
        protected void onMethodEnter() {
            // long count = AgentStats.recordInvocation();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "sentinel/AgentStats", "recordInvocation", "()J", false);
            int countVar = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, countVar);

            // System.out.println("[Sentinel] " + methodName + "() call #" + count);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("[Sentinel] " + methodName + "() call #");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(Opcodes.LLOAD, countVar);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(J)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
        }

        @Override
        protected void onMethodExit(int opcode) {
            // no-op for COUNT mode
        }
    }

    // ─── Agent entrypoints ───────────────────────────────────────────────────

    /**
     * Premain agent entrypoint invoked by the JVM when starting with {@code -javaagent}.
     *
     * @param agentArgs       comma-separated {@code key=value} configuration arguments
     * @param instrumentation the JVM instrumentation service
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        AgentConfig config = AgentConfig.parse(agentArgs);
        System.out.println("[Sentinel] ─────────────────────────────────────────────");
        System.out.println("[Sentinel] Sentinel Java Agent v1.0.0 — by JOJIN JOHN");
        System.out.println("[Sentinel] " + config);
        System.out.println("[Sentinel] ─────────────────────────────────────────────");

        instrumentation.addTransformer(new Loader(config), true);

        // Print stats summary on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            AgentStats.printSummary();
        }, "sentinel-stats-hook"));
    }

    /**
     * Agentmain entrypoint for dynamic attach via the Attach API.
     *
     * @param agentArgs       comma-separated {@code key=value} configuration arguments
     * @param instrumentation the JVM instrumentation service
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        premain(agentArgs, instrumentation);
    }
}
