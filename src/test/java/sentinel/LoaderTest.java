package sentinel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Loader Agent and Bytecode Transformation Tests")
class LoaderTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Creates a Loader configured for REPLACE mode on the demo target. */
    private static Loader replaceLoader() {
        AgentConfig config = AgentConfig.parse(null); // defaults: REPLACE, demo/target/TargetService
        return new Loader(config);
    }

    /** Creates a Loader configured for TRACE mode on the demo target. */
    private static Loader traceLoader() {
        AgentConfig config = AgentConfig.parse("mode=TRACE");
        return new Loader(config);
    }

    /** Creates a Loader configured for COUNT mode on the demo target. */
    private static Loader countLoader() {
        AgentConfig config = AgentConfig.parse("mode=COUNT");
        return new Loader(config);
    }

    private byte[] loadTargetServiceBytecode() throws IOException {
        String resourcePath = "demo/target/TargetService.class";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }

    // ─── AgentConfig tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("AgentConfig: default values when agentArgs is null")
    void testDefaultConfig() {
        AgentConfig config = AgentConfig.parse(null);
        assertEquals(AgentConfig.DEFAULT_TARGET, config.getTargetClass());
        assertEquals(AgentConfig.DEFAULT_METHOD,  config.getTargetMethod());
        assertEquals(TransformMode.REPLACE,        config.getMode());
        assertEquals(false,                        config.isVerbose());
    }

    @Test
    @DisplayName("AgentConfig: parses all keys from agentArgs string")
    void testParsedConfig() {
        AgentConfig config = AgentConfig.parse("target=com/example/Foo,method=bar,mode=TRACE,verbose=true");
        assertEquals("com/example/Foo", config.getTargetClass());
        assertEquals("bar",              config.getTargetMethod());
        assertEquals(TransformMode.TRACE, config.getMode());
        assertEquals(true,               config.isVerbose());
    }

    @Test
    @DisplayName("AgentConfig: dot-notation target class is converted to slash notation")
    void testDotNotationConversion() {
        AgentConfig config = AgentConfig.parse("target=com.example.MyService");
        assertEquals("com/example/MyService", config.getTargetClass());
    }

    @Test
    @DisplayName("AgentConfig: unknown mode falls back to REPLACE")
    void testUnknownModeDefaultsToReplace() {
        AgentConfig config = AgentConfig.parse("mode=INVALID_MODE");
        assertEquals(TransformMode.REPLACE, config.getMode());
    }

    // ─── Null-safety ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Null safety: transform returns null for null or empty inputs")
    void testNullAndEmptyInputs() {
        Loader loader = replaceLoader();
        assertNull(loader.transform(null, null, null, null, null));
        assertNull(loader.transform(null, "demo/target/TargetService", null, null, null));
        assertNull(loader.transform(null, "demo/target/TargetService", null, null, new byte[0]));
    }

    // ─── Scope gating ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Scope gating: transform returns null for unrelated classes")
    void testUnrelatedClassesIgnored() {
        Loader loader = replaceLoader();
        byte[] dummyBytecode = new byte[]{1, 2, 3};
        assertNull(loader.transform(null, "java/lang/String",         null, null, dummyBytecode));
        assertNull(loader.transform(null, "sentinel/Loader",          null, null, dummyBytecode));
        assertNull(loader.transform(null, "sentinel/Filter",          null, null, dummyBytecode));
        assertNull(loader.transform(null, "demo/app/DemoApplication", null, null, dummyBytecode));
        assertNull(loader.transform(null, "com/example/RandomService",null, null, dummyBytecode));
    }

    // ─── REPLACE mode ────────────────────────────────────────────────────────

    @Test
    @DisplayName("REPLACE mode: successfully modifies message() while preserving add()")
    void testReplaceTransformation() throws Exception {
        byte[] originalBytecode = loadTargetServiceBytecode();
        assertNotNull(originalBytecode, "Could not locate TargetService.class resource");

        Loader loader = replaceLoader();
        byte[] transformedBytecode = loader.transform(
                getClass().getClassLoader(),
                "demo/target/TargetService",
                null, null,
                originalBytecode
        );

        assertNotNull(transformedBytecode, "Transformed bytecode should not be null");

        ByteArrayClassLoader customLoader = new ByteArrayClassLoader(
                getClass().getClassLoader(),
                "demo.target.TargetService",
                transformedBytecode
        );

        Class<?> transformedClass = customLoader.loadClass("demo.target.TargetService");
        Object instance = transformedClass.getDeclaredConstructor().newInstance();

        Method messageMethod = transformedClass.getMethod("message");
        assertEquals("Transformed message", messageMethod.invoke(instance),
                "message() must return 'Transformed message'");

        Method addMethod = transformedClass.getMethod("add", int.class, int.class);
        assertEquals(40, addMethod.invoke(instance, 15, 25),
                "add() must retain original logic and return 40");
    }

    @Test
    @DisplayName("REPLACE mode: idempotency — retransforming already-replaced class returns null")
    void testIdempotentRetransformation() throws Exception {
        byte[] originalBytecode = loadTargetServiceBytecode();
        assertNotNull(originalBytecode);

        Loader loader = replaceLoader();

        byte[] firstPass = loader.transform(
                getClass().getClassLoader(), "demo/target/TargetService",
                null, null, originalBytecode);
        assertNotNull(firstPass, "First transformation must succeed");

        byte[] secondPass = loader.transform(
                getClass().getClassLoader(), "demo/target/TargetService",
                null, null, firstPass);
        assertNull(secondPass, "Subsequent transformation must return null (idempotency)");
    }

    // ─── TRACE mode ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("TRACE mode: produces valid bytecode for TargetService")
    void testTraceModeProducesValidBytecode() throws Exception {
        byte[] originalBytecode = loadTargetServiceBytecode();
        assertNotNull(originalBytecode);

        Loader loader = traceLoader();
        byte[] traced = loader.transform(
                getClass().getClassLoader(), "demo/target/TargetService",
                null, null, originalBytecode);

        assertNotNull(traced, "TRACE mode must produce transformed bytecode");

        ByteArrayClassLoader customLoader = new ByteArrayClassLoader(
                getClass().getClassLoader(), "demo.target.TargetService", traced);
        Class<?> cls = customLoader.loadClass("demo.target.TargetService");
        Object instance = cls.getDeclaredConstructor().newInstance();

        // Original method still returns the original string in TRACE mode (only probes added)
        Method msg = cls.getMethod("message");
        assertEquals("Original message", msg.invoke(instance),
                "TRACE mode must NOT change return value");
    }

    // ─── COUNT mode ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("COUNT mode: produces valid bytecode for TargetService")
    void testCountModeProducesValidBytecode() throws Exception {
        byte[] originalBytecode = loadTargetServiceBytecode();
        assertNotNull(originalBytecode);

        Loader loader = countLoader();
        byte[] counted = loader.transform(
                getClass().getClassLoader(), "demo/target/TargetService",
                null, null, originalBytecode);

        assertNotNull(counted, "COUNT mode must produce transformed bytecode");

        ByteArrayClassLoader customLoader = new ByteArrayClassLoader(
                getClass().getClassLoader(), "demo.target.TargetService", counted);
        Class<?> cls = customLoader.loadClass("demo.target.TargetService");
        Object instance = cls.getDeclaredConstructor().newInstance();

        Method msg = cls.getMethod("message");
        assertEquals("Original message", msg.invoke(instance),
                "COUNT mode must NOT change return value");
    }

    // ─── ByteArrayClassLoader ────────────────────────────────────────────────

    /**
     * Isolated child-first ClassLoader to load and verify generated bytecode at runtime.
     */
    private static class ByteArrayClassLoader extends ClassLoader {
        private final String targetClassName;
        private final byte[] classBytes;

        ByteArrayClassLoader(ClassLoader parent, String targetClassName, byte[] classBytes) {
            super(parent);
            this.targetClassName = targetClassName;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (targetClassName.equals(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) c = findClass(name);
                if (resolve) resolveClass(c);
                return c;
            }
            return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (targetClassName.equals(name)) {
                return defineClass(name, classBytes, 0, classBytes.length);
            }
            return super.findClass(name);
        }
    }
}
