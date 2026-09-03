package sentinel;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
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
 *
 * @author JOJIN JOHN
 */
public final class Loader implements ClassFileTransformer {

    private static final String TARGET_CLASS_INTERNAL = "demo/target/TargetService";
    private static final String TARGET_METHOD_NAME = "message";
    private static final String TARGET_METHOD_DESC = "()Ljava/lang/String;";
    private static final String TRANSFORMED_MESSAGE = "Transformed message";

    /**
     * Constructs a new {@code Loader} transformer instance.
     */
    public Loader() {
        // Stateless, thread-safe transformer
    }

    /**
     * Intercepts and selectively transforms class byte streams during class loading or retransformation.
     *
     * @param loader              the defining loader of the class to be transformed
     * @param className           the internal JVM name of the class (e.g., {@code demo/target/TargetService})
     * @param classBeingRedefined if this is triggered by a redefine or retransform, the class being redefined
     * @param protectionDomain    the protection domain of the class being defined or redefined
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

        // 2. Strict scope gating: only process the controlled demo target
        if (!TARGET_CLASS_INTERNAL.equals(className)) {
            return null;
        }

        System.out.println("[Sentinel] Inspecting demo.target.TargetService");

        try {
            // 3. Parse bytecode using ASM Tree API
            // SKIP_FRAMES discards existing stack maps since ClassWriter.COMPUTE_FRAMES recomputes them cleanly
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_FRAMES);

            boolean modified = false;

            // 4. Locate and transform the target method
            for (MethodNode method : classNode.methods) {
                if (TARGET_METHOD_NAME.equals(method.name) && TARGET_METHOD_DESC.equals(method.desc)) {
                    // 5. Idempotency guard: avoid duplicating transformation during class retransformation
                    if (isAlreadyTransformed(method)) {
                        System.out.println("[Sentinel] TargetService.message() already transformed; skipping duplicate instrumentation");
                        return null;
                    }

                    System.out.println("[Sentinel] Transforming TargetService.message()");

                    // 6. Build replacement instructions using symbolic Opcodes constants
                    InsnList newInstructions = new InsnList();
                    newInstructions.add(new LdcInsnNode(TRANSFORMED_MESSAGE));
                    newInstructions.add(new InsnNode(Opcodes.ARETURN));

                    // Replace method body
                    method.instructions.clear();
                    method.instructions.add(newInstructions);

                    modified = true;
                    break;
                }
            }

            if (!modified) {
                return null;
            }

            // 7. Recompute stack map frames and max stack/locals cleanly
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            byte[] transformedBytes = writer.toByteArray();

            System.out.println("[Sentinel] Transformation complete");
            return transformedBytes;

        } catch (Throwable t) {
            System.err.println("[Sentinel] Unexpected error transforming " + className + ": " + t.getMessage());
            t.printStackTrace(System.err);
            // Returning null causes the JVM to continue using the uninstrumented bytecode
            return null;
        }
    }

    /**
     * Inspects method instructions to check if the target transformation constant is already present.
     * Guarantees idempotency when {@code retransformClasses} is invoked.
     *
     * @param method the method node to inspect
     * @return {@code true} if already instrumented, {@code false} otherwise
     */
    private static boolean isAlreadyTransformed(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LdcInsnNode ldc && TRANSFORMED_MESSAGE.equals(ldc.cst)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Premain agent entrypoint invoked by the JVM when starting with {@code -javaagent}.
     *
     * @param agentArgs       command-line arguments passed to the agent
     * @param instrumentation the JVM instrumentation service
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[Sentinel] Agent initialized (retransformation support: true)");
        // Passing canRetransform = true ensures retransformClasses() triggers this transformer
        instrumentation.addTransformer(new Loader(), true);
    }
}
