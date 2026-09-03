package sentinel;

/**
 * Enumeration of supported bytecode transformation modes.
 *
 * <ul>
 *   <li>{@link #REPLACE}    — replaces the return value of a target method with a constant string</li>
 *   <li>{@link #TRACE}      — injects entry/exit logging with nanosecond timing and return value capture</li>
 *   <li>{@link #COUNT}      — injects an atomic invocation counter that prints on each call</li>
 *   <li>{@link #NULL_CHECK} — injects a null-return guard that logs a warning when a method returns null</li>
 * </ul>
 *
 * @author JOJIN JOHN
 */
public enum TransformMode {

    /**
     * Replaces the target method body to return a constant {@code "Transformed message"} string.
     * Demonstrates basic LDC + ARETURN bytecode injection.
     */
    REPLACE,

    /**
     * Injects {@code System.nanoTime()} probes at method entry and exit.
     * Captures and logs the return value for reference-type methods.
     * Prints elapsed time in milliseconds via stdout and/or log file.
     */
    TRACE,

    /**
     * Injects a static {@code AtomicInteger} counter increment at method entry.
     * Prints the current invocation count on each call.
     */
    COUNT,

    /**
     * Injects a post-return null check for reference-returning methods.
     * Logs a warning to stderr if the method returns {@code null}.
     * The method's original behaviour and return value are preserved.
     */
    NULL_CHECK
}
