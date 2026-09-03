package sentinel;

/**
 * Enumeration of supported bytecode transformation modes.
 *
 * <ul>
 *   <li>{@link #REPLACE} — replaces the return value of a target method with a constant string</li>
 *   <li>{@link #TRACE}   — injects entry/exit logging with nanosecond timing around a method</li>
 *   <li>{@link #COUNT}   — injects an atomic invocation counter that prints on each call</li>
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
     * Prints elapsed time in milliseconds via stdout.
     */
    TRACE,

    /**
     * Injects a static {@code AtomicInteger} counter increment at method entry.
     * Prints the current invocation count on each call.
     */
    COUNT
}
