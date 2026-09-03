package sentinel;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Parses and holds configuration supplied via {@code -javaagent} arguments or a properties file.
 * <p>
 * Accepts a comma-separated {@code key=value} string, e.g.:
 * <pre>
 *   -javaagent:sentinel-agent.jar=target=demo/target/*,method=message,mode=TRACE,verbose=true,logfile=sentinel.log
 * </pre>
 * To load from a file:
 * <pre>
 *   -javaagent:sentinel-agent.jar=config=/path/to/sentinel.properties
 * </pre>
 *
 * <h3>Supported Keys</h3>
 * <table border="1">
 *   <tr><th>Key</th><th>Description</th><th>Default</th></tr>
 *   <tr><td>{@code target}</td><td>Comma-separated list of class patterns (glob supported)</td>
 *       <td>{@code demo/target/TargetService}</td></tr>
 *   <tr><td>{@code method}</td><td>Method name to target</td><td>{@code message}</td></tr>
 *   <tr><td>{@code mode}</td><td>REPLACE | TRACE | COUNT | NULL_CHECK</td><td>{@code REPLACE}</td></tr>
 *   <tr><td>{@code verbose}</td><td>Enable verbose scan logging</td><td>{@code false}</td></tr>
 *   <tr><td>{@code logfile}</td><td>Path to output log file (appended)</td><td>none</td></tr>
 *   <tr><td>{@code config}</td><td>Path to a .properties config file</td><td>none</td></tr>
 * </table>
 *
 * @author JOJIN JOHN
 */
public final class AgentConfig {

    /** Default target class pattern. */
    public static final String DEFAULT_TARGET  = "demo/target/TargetService";
    /** Default method name. */
    public static final String DEFAULT_METHOD  = "message";
    /** Default transformation mode. */
    public static final String DEFAULT_MODE    = "REPLACE";

    private final List<PatternMatcher> targetMatchers;
    private final String targetMethod;
    private final TransformMode mode;
    private final boolean verbose;
    private final String logFile;

    private AgentConfig(List<PatternMatcher> matchers, String targetMethod,
                        TransformMode mode, boolean verbose, String logFile) {
        this.targetMatchers = Collections.unmodifiableList(matchers);
        this.targetMethod   = targetMethod;
        this.mode           = mode;
        this.verbose        = verbose;
        this.logFile        = logFile;
    }

    /**
     * Parses agent arguments into an {@link AgentConfig}.
     * If {@code config=<path>} is present, the properties file is loaded first,
     * then inline arguments override any file values.
     *
     * @param agentArgs the raw agent arguments string (may be {@code null})
     * @return a fully populated {@code AgentConfig}
     */
    public static AgentConfig parse(String agentArgs) {
        Map<String, String> map = new HashMap<>();

        // 1. Load from inline args
        if (agentArgs != null && !agentArgs.isBlank()) {
            for (String part : agentArgs.split(",(?=[^,]+=)")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
        }

        // 2. If config file specified, load it (inline args override file values)
        String configPath = map.get("config");
        if (configPath != null) {
            Properties props = loadPropertiesFile(configPath);
            for (String key : props.stringPropertyNames()) {
                // normalize key: strip "sentinel." prefix if present
                String normalizedKey = key.startsWith("sentinel.") ? key.substring(9) : key;
                map.putIfAbsent(normalizedKey, props.getProperty(key));
            }
        }

        // 3. Parse targets (comma-separated patterns)
        String targetRaw = map.getOrDefault("target", DEFAULT_TARGET);
        List<PatternMatcher> matchers = new ArrayList<>();
        for (String pattern : targetRaw.split(";")) {
            String p = pattern.trim().replace('.', '/');
            if (!p.isBlank()) {
                matchers.add(new PatternMatcher(p));
            }
        }
        if (matchers.isEmpty()) {
            matchers.add(new PatternMatcher(DEFAULT_TARGET));
        }

        // 4. Parse other values
        String method  = map.getOrDefault("method",  DEFAULT_METHOD);
        boolean verbose = Boolean.parseBoolean(map.getOrDefault("verbose", "false"));
        String logFile  = map.getOrDefault("logfile", null);

        TransformMode mode;
        try {
            mode = TransformMode.valueOf(map.getOrDefault("mode", DEFAULT_MODE).toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[Sentinel] Unknown mode '" + map.get("mode") + "' — falling back to REPLACE");
            mode = TransformMode.REPLACE;
        }

        return new AgentConfig(matchers, method, mode, verbose, logFile);
    }

    /**
     * Returns whether the given internal class name matches any of the configured target patterns.
     *
     * @param internalClassName JVM internal class name (slash-separated)
     * @return {@code true} if the class should be instrumented
     */
    public boolean matches(String internalClassName) {
        for (PatternMatcher matcher : targetMatchers) {
            if (matcher.matches(internalClassName)) {
                return true;
            }
        }
        return false;
    }

    /** @return unmodifiable list of compiled target pattern matchers */
    public List<PatternMatcher> getTargetMatchers()  { return targetMatchers; }

    /** @return target method name */
    public String getTargetMethod()                  { return targetMethod; }

    /** @return active transformation mode */
    public TransformMode getMode()                   { return mode; }

    /** @return whether verbose scan logging is enabled */
    public boolean isVerbose()                       { return verbose; }

    /** @return path to log file, or {@code null} if file logging is disabled */
    public String getLogFile()                       { return logFile; }

    @Override
    public String toString() {
        List<String> patterns = new ArrayList<>();
        for (PatternMatcher m : targetMatchers) {
            patterns.add(m.getPattern());
        }
        return String.format("AgentConfig{targets=%s, method='%s', mode=%s, verbose=%s, logfile=%s}",
                patterns, targetMethod, mode, verbose, logFile != null ? logFile : "none");
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private static Properties loadPropertiesFile(String path) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
            System.out.println("[Sentinel] Loaded config file: " + path);
        } catch (IOException e) {
            System.err.println("[Sentinel] Could not load config file '" + path + "': " + e.getMessage());
        }
        return props;
    }
}
