package sentinel;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses and holds configuration supplied via {@code -javaagent} arguments.
 * <p>
 * Accepts a comma-separated {@code key=value} string, e.g.:
 * <pre>
 *   -javaagent:sentinel-agent.jar=target=demo/target/TargetService,method=message,mode=REPLACE
 * </pre>
 * Supported keys:
 * <ul>
 *   <li>{@code target}  — internal JVM class name to instrument (slash-separated)</li>
 *   <li>{@code method}  — method name to target (default: {@code message})</li>
 *   <li>{@code mode}    — transformation mode: {@code REPLACE}, {@code TRACE}, {@code COUNT} (default: {@code REPLACE})</li>
 *   <li>{@code verbose} — {@code true} to enable verbose logging (default: {@code false})</li>
 * </ul>
 *
 * @author JOJIN JOHN
 */
public final class AgentConfig {

    /** Default target class if none provided. */
    public static final String DEFAULT_TARGET = "demo/target/TargetService";
    /** Default method name. */
    public static final String DEFAULT_METHOD  = "message";
    /** Default transformation mode. */
    public static final String DEFAULT_MODE    = "REPLACE";

    private final String targetClass;
    private final String targetMethod;
    private final TransformMode mode;
    private final boolean verbose;

    private AgentConfig(String targetClass, String targetMethod, TransformMode mode, boolean verbose) {
        this.targetClass  = targetClass;
        this.targetMethod = targetMethod;
        this.mode         = mode;
        this.verbose      = verbose;
    }

    /**
     * Parses agent arguments into an {@link AgentConfig}.
     *
     * @param agentArgs the raw agent arguments string (may be {@code null})
     * @return a fully populated {@code AgentConfig}
     */
    public static AgentConfig parse(String agentArgs) {
        Map<String, String> map = new HashMap<>();
        if (agentArgs != null && !agentArgs.isBlank()) {
            for (String part : agentArgs.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
        }

        String target  = map.getOrDefault("target",  DEFAULT_TARGET).replace('.', '/');
        String method  = map.getOrDefault("method",  DEFAULT_METHOD);
        boolean verbose = Boolean.parseBoolean(map.getOrDefault("verbose", "false"));

        TransformMode mode;
        try {
            mode = TransformMode.valueOf(map.getOrDefault("mode", DEFAULT_MODE).toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[Sentinel] Unknown mode '" + map.get("mode") + "' — falling back to REPLACE");
            mode = TransformMode.REPLACE;
        }

        return new AgentConfig(target, method, mode, verbose);
    }

    /** @return internal JVM class name of the instrumentation target */
    public String getTargetClass()  { return targetClass; }

    /** @return target method name */
    public String getTargetMethod() { return targetMethod; }

    /** @return active transformation mode */
    public TransformMode getMode()  { return mode; }

    /** @return whether verbose logging is enabled */
    public boolean isVerbose()      { return verbose; }

    @Override
    public String toString() {
        return String.format("AgentConfig{target='%s', method='%s', mode=%s, verbose=%s}",
                targetClass, targetMethod, mode, verbose);
    }
}
