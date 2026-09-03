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
 * Supports four transformation modes (see {@link TransformMode}):
 * <ul>
 *   <li>{@code REPLACE}    — replaces the return value with a constant string</li>
 *   <li>{@code TRACE}      — injects entry/exit timing probes and logs return values</li>
 *   <li>{@code COUNT}      — injects an atomic invocation counter</li>
 *   <li>{@code NULL_CHECK} — injects a post-return null guard with stderr warning</li>
 * </ul>
 * Multiple target classes and glob wildcard patterns are supported via {@link AgentConfig}.
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

        // 2. Multi-target scope gating with wildcard support
        if (!config.matches(className)) {
            return null;
        }

        if (config.isVerbose()) {
            AgentLogger.info("Inspecting: " + className.replace('/', '.'));
        }

        try {
            byte[] result = switch (config.getMode()) {
                case REPLACE    -> applyReplace(classfileBuffer);
                case TRACE      -> applyTrace(classfileBuffer);
                case COUNT      -> applyCount(classfileBuffer);
                case NULL_CHECK -> applyNullCheck(classfileBuffer);
            };

            if (result != null) {
                AgentStats.recordTransform();
                AgentLogger.info("Transformed: " + className.replace('/', '.')
                        + " [mode=" + config.getMode() + "]");
            }
            return result;

        } catch (Throwable t) {
            AgentStats.recordError();
            AgentLogger.error("Error transforming " + className + ": " + t.getMessage());
            t.printStackTrace(System.err);
            return null;
        }
    }

    // ─── REPLACE mode ────────────────────────────────────────────────────────

    /**
     * Replaces the return value of the target method with a constant {@code "Transformed message"}.
     *
     * @param classfileBuffer original bytecode
     * @return transformed bytecode, or {@code null} if no change made
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
                        AgentLogger.info("Already replaced; skipping.");
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
     * Injects entry/exit timing probes and return value logging around the target method.
     *
     * @param classfileBuffer original bytecode
     * @return transformed bytecode
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

    /** Injects entry/exit timing probes and captures the return value for reference types. */
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

            // System.out.println("[Sentinel] ENTER methodName()");
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("[Sentinel] ENTER " + methodName + "()");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == Opcodes.ATHROW) {
                return;
            }

            // Capture return value for reference-returning methods (Feature 6)
            int retVar = -1;
            if (opcode == Opcodes.ARETURN) {
                // Stack: [returnValue] — DUP it so we can log and still return
                mv.visitInsn(Opcodes.DUP);
                retVar = newLocal(Type.getType(Object.class));
                mv.visitVarInsn(Opcodes.ASTORE, retVar);
            }

            // Calculate elapsed ms: (System.nanoTime() - startTime) / 1_000_000
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            mv.visitVarInsn(Opcodes.LLOAD, startTimeVar);
            mv.visitInsn(Opcodes.LSUB);
            mv.visitLdcInsn(1_000_000L);
            mv.visitInsn(Opcodes.LDIV);
            int msVar = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, msVar);

            // Build and print exit message
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("[Sentinel] EXIT  " + methodName + "()");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

            if (retVar >= 0) {
                // Append " — returned: <value>"
                mv.visitLdcInsn(" — returned: ");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mv.visitVarInsn(Opcodes.ALOAD, retVar);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf",
                        "(Ljava/lang/Object;)Ljava/lang/String;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            }

            // Append " — took Xms"
            mv.visitLdcInsn(" — took ");
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
     * Injects an {@link AgentStats#recordInvocation()} call at the entry of the target method.
     *
     * @param classfileBuffer original bytecode
     * @return transformed bytecode
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

    /** Injects an atomic invocation counter at method entry. */
    private static final class CountMethodAdapter extends AdviceAdapter {

        private final String methodName;

        CountMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
            this.methodName = name;
        }

        @Override
        protected void onMethodEnter() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "sentinel/AgentStats", "recordInvocation", "()J", false);
            int countVar = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, countVar);

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
        protected void onMethodExit(int opcode) {}
    }

    // ─── NULL_CHECK mode ─────────────────────────────────────────────────────

    /**
     * Injects a post-return null guard: logs a warning to stderr if the target method returns null.
     *
     * @param classfileBuffer original bytecode
     * @return transformed bytecode
     */
    private byte[] applyNullCheck(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        String targetMethod = config.getTargetMethod();

        reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (targetMethod.equals(name)) {
                    return new NullCheckMethodAdapter(Opcodes.ASM9, mv, access, name, descriptor);
                }
                return mv;
            }
        }, ClassReader.SKIP_FRAMES);

        return writer.toByteArray();
    }

    /** Injects a null-return guard at every ARETURN in the target method. */
    private static final class NullCheckMethodAdapter extends AdviceAdapter {

        private final String methodName;

        NullCheckMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
            this.methodName = name;
        }

        @Override
        protected void onMethodEnter() {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("[Sentinel] NULL_CHECK active on " + methodName + "()");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode != Opcodes.ARETURN) {
                return; // Only check reference-returning methods
            }

            // Stack: [returnValue]
            mv.visitInsn(Opcodes.DUP);
            // Stack: [returnValue, returnValue]

            Label notNull = new Label();
            mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
            // returnValue IS null — log warning to stderr
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("[Sentinel] NULL_CHECK WARNING: " + methodName + "() returned null!");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
            mv.visitLabel(notNull);
            // Stack: [returnValue] — original return value preserved
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
        AgentLogger.init(config.getLogFile());

        AgentLogger.info("─────────────────────────────────────────────");
        AgentLogger.info("Sentinel Java Agent v1.0.0 — by JOJIN JOHN");
        AgentLogger.info(config.toString());
        AgentLogger.info("─────────────────────────────────────────────");

        instrumentation.addTransformer(new Loader(config), true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            AgentStats.printSummary();
            AgentLogger.close();
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
