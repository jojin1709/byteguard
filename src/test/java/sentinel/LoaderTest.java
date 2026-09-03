package sentinel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Loader Agent and Bytecode Transformation Tests")
class LoaderTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Loader loaderFor(String agentArgs) {
        return new Loader(AgentConfig.parse(agentArgs));
    }

    private byte[] loadTargetServiceBytecode() throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("demo/target/TargetService.class")) {
            return in != null ? in.readAllBytes() : null;
        }
    }

    // ─── AgentConfig tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("AgentConfig: defaults when agentArgs is null")
    void testDefaultConfig() {
        AgentConfig cfg = AgentConfig.parse(null);
        assertEquals(AgentConfig.DEFAULT_METHOD,  cfg.getTargetMethod());
        assertEquals(TransformMode.REPLACE,        cfg.getMode());
        assertFalse(cfg.isVerbose());
        assertNull(cfg.getLogFile());
        assertTrue(cfg.matches(AgentConfig.DEFAULT_TARGET));
    }

    @Test
    @DisplayName("AgentConfig: parses mode, method, verbose, logfile")
    void testParsedConfig() {
        AgentConfig cfg = AgentConfig.parse(
                "target=com/example/Foo,method=bar,mode=TRACE,verbose=true,logfile=test.log");
        assertTrue(cfg.matches("com/example/Foo"));
        assertEquals("bar",              cfg.getTargetMethod());
        assertEquals(TransformMode.TRACE, cfg.getMode());
        assertTrue(cfg.isVerbose());
        assertEquals("test.log",         cfg.getLogFile());
    }

    @Test
    @DisplayName("AgentConfig: dot-notation target converted to slash")
    void testDotNotation() {
        AgentConfig cfg = AgentConfig.parse("target=com.example.MyService");
        assertTrue(cfg.matches("com/example/MyService"));
    }

    @Test
    @DisplayName("AgentConfig: unknown mode falls back to REPLACE")
    void testUnknownMode() {
        AgentConfig cfg = AgentConfig.parse("mode=INVALID_MODE");
        assertEquals(TransformMode.REPLACE, cfg.getMode());
    }

    @Test
    @DisplayName("AgentConfig: NULL_CHECK mode parses correctly")
    void testAgentConfigNullCheckMode() {
        AgentConfig cfg = AgentConfig.parse("mode=NULL_CHECK");
        assertEquals(TransformMode.NULL_CHECK, cfg.getMode());
    }

    // ─── PatternMatcher tests ────────────────────────────────────────────────

    @Test
    @DisplayName("PatternMatcher: exact match")
    void testExactMatch() {
        PatternMatcher pm = new PatternMatcher("demo/target/TargetService");
        assertTrue(pm.matches("demo/target/TargetService"));
        assertFalse(pm.matches("demo/target/OtherService"));
    }

    @Test
    @DisplayName("PatternMatcher: single * does not cross slash boundary")
    void testSingleWildcard() {
        PatternMatcher pm = new PatternMatcher("demo/target/*");
        assertTrue(pm.matches("demo/target/TargetService"));
        assertTrue(pm.matches("demo/target/FooService"));
        assertFalse(pm.matches("demo/target/sub/Deep"));
        assertFalse(pm.matches("demo/other/TargetService"));
    }

    @Test
    @DisplayName("PatternMatcher: ** crosses slash boundaries")
    void testDoubleWildcard() {
        PatternMatcher pm = new PatternMatcher("demo/**");
        assertTrue(pm.matches("demo/target/TargetService"));
        assertTrue(pm.matches("demo/app/DemoApplication"));
        assertTrue(pm.matches("demo/a/b/c/Deep"));
        assertFalse(pm.matches("com/example/Other"));
    }

    @Test
    @DisplayName("PatternMatcher: prefix wildcard")
    void testPrefixWildcard() {
        PatternMatcher pm = new PatternMatcher("demo/target/Target*");
        assertTrue(pm.matches("demo/target/TargetService"));
        assertTrue(pm.matches("demo/target/TargetRepo"));
        assertFalse(pm.matches("demo/target/FooService"));
    }

    // ─── Multiple targets ────────────────────────────────────────────────────

    @Test
    @DisplayName("AgentConfig: multiple semicolon-separated targets")
    void testMultipleTargets() {
        AgentConfig cfg = AgentConfig.parse("target=demo/target/TargetService;demo/app/DemoApplication");
        assertTrue(cfg.matches("demo/target/TargetService"));
        assertTrue(cfg.matches("demo/app/DemoApplication"));
        assertFalse(cfg.matches("com/example/Other"));
    }

    // ─── Null-safety ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Null safety: transform returns null for null/empty inputs")
    void testNullSafety() {
        Loader loader = loaderFor(null);
        assertNull(loader.transform(null, null, null, null, null));
        assertNull(loader.transform(null, "demo/target/TargetService", null, null, null));
        assertNull(loader.transform(null, "demo/target/TargetService", null, null, new byte[0]));
    }

    @Test
    @DisplayName("Scope gating: unrelated classes return null")
    void testScopeGating() {
        Loader loader = loaderFor(null);
        byte[] dummy = new byte[]{1, 2, 3};
        assertNull(loader.transform(null, "java/lang/String",          null, null, dummy));
        assertNull(loader.transform(null, "sentinel/Loader",           null, null, dummy));
        assertNull(loader.transform(null, "com/example/RandomService", null, null, dummy));
    }

    // ─── REPLACE mode ────────────────────────────────────────────────────────

    @Test
    @DisplayName("REPLACE: message() returns 'Transformed message', add() preserved")
    void testReplaceMode() throws Exception {
        byte[] original = loadTargetServiceBytecode();
        assertNotNull(original);

        Loader loader = loaderFor(null);
        byte[] transformed = loader.transform(
                getClass().getClassLoader(), "demo/target/TargetService", null, null, original);
        assertNotNull(transformed);

        Object instance = loadClass("demo.target.TargetService", transformed)
                .getDeclaredConstructor().newInstance();

        assertEquals("Transformed message",
                getMethod(instance, "message").invoke(instance));
        assertEquals(40,
                getMethod(instance, "add", int.class, int.class).invoke(instance, 15, 25));
    }

    @Test
    @DisplayName("REPLACE: idempotency — second transformation returns null")
    void testReplaceIdempotency() throws Exception {
        byte[] original = loadTargetServiceBytecode();
        Loader loader = loaderFor(null);

        byte[] first = loader.transform(getClass().getClassLoader(),
                "demo/target/TargetService", null, null, original);
        assertNotNull(first);

        byte[] second = loader.transform(getClass().getClassLoader(),
                "demo/target/TargetService", null, null, first);
        assertNull(second);
    }

    // ─── TRACE mode ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("TRACE: produces valid bytecode, preserves return value")
    void testTraceMode() throws Exception {
        byte[] original = loadTargetServiceBytecode();
        Loader loader = loaderFor("mode=TRACE");

        byte[] traced = loader.transform(getClass().getClassLoader(),
                "demo/target/TargetService", null, null, original);
        assertNotNull(traced);

        Object instance = loadClass("demo.target.TargetService", traced)
                .getDeclaredConstructor().newInstance();
        assertEquals("Original message",
                getMethod(instance, "message").invoke(instance),
                "TRACE must NOT alter return value");
    }

    // ─── COUNT mode ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("COUNT: produces valid bytecode, preserves return value")
    void testCountMode() throws Exception {
        byte[] original = loadTargetServiceBytecode();
        Loader loader = loaderFor("mode=COUNT");

        byte[] counted = loader.transform(getClass().getClassLoader(),
                "demo/target/TargetService", null, null, original);
        assertNotNull(counted);

        Object instance = loadClass("demo.target.TargetService", counted)
                .getDeclaredConstructor().newInstance();
        assertEquals("Original message",
                getMethod(instance, "message").invoke(instance));
    }

    // ─── NULL_CHECK mode ─────────────────────────────────────────────────────

    @Test
    @DisplayName("NULL_CHECK: produces valid bytecode, preserves non-null return value")
    void testNullCheckMode() throws Exception {
        byte[] original = loadTargetServiceBytecode();
        Loader loader = loaderFor("mode=NULL_CHECK");

        byte[] checked = loader.transform(getClass().getClassLoader(),
                "demo/target/TargetService", null, null, original);
        assertNotNull(checked);

        Object instance = loadClass("demo.target.TargetService", checked)
                .getDeclaredConstructor().newInstance();
        assertEquals("Original message",
                getMethod(instance, "message").invoke(instance),
                "NULL_CHECK must NOT alter non-null return value");
    }

    // ─── AgentLogger tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("AgentLogger: init with null logfile does not throw")
    void testLoggerNullLogFile() {
        assertDoesNotThrow(() -> AgentLogger.init(null));
        assertDoesNotThrow(() -> AgentLogger.info("test info"));
        assertDoesNotThrow(() -> AgentLogger.warn("test warn"));
        assertDoesNotThrow(() -> AgentLogger.error("test error"));
        assertDoesNotThrow(() -> AgentLogger.close());
    }

    // ─── AgentStats tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("AgentStats: counters increment correctly")
    void testAgentStats() {
        int scansBefore  = AgentStats.getClassesScanned();
        int transsBefore = AgentStats.getClassesTransformed();

        AgentStats.recordScan();
        AgentStats.recordScan();
        AgentStats.recordTransform();

        assertEquals(scansBefore  + 2, AgentStats.getClassesScanned());
        assertEquals(transsBefore + 1, AgentStats.getClassesTransformed());
        assertTrue(AgentStats.getMethodInvocations() >= 0);
    }

    // ─── Utilities ───────────────────────────────────────────────────────────

    private Class<?> loadClass(String name, byte[] bytecode) {
        return new ByteArrayClassLoader(getClass().getClassLoader(), name, bytecode)
                .loadClassDirectly(name, bytecode);
    }

    private Method getMethod(Object instance, String name, Class<?>... params) throws Exception {
        return instance.getClass().getMethod(name, params);
    }

    private static class ByteArrayClassLoader extends ClassLoader {
        private final String targetName;
        private final byte[] bytes;

        ByteArrayClassLoader(ClassLoader parent, String name, byte[] bytes) {
            super(parent);
            this.targetName = name;
            this.bytes = bytes;
        }

        Class<?> loadClassDirectly(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (targetName.equals(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) c = defineClass(name, bytes, 0, bytes.length);
                if (resolve) resolveClass(c);
                return c;
            }
            return super.loadClass(name, resolve);
        }
    }
}
