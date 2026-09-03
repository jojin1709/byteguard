package sentinel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and reports runtime transformation statistics for the Sentinel Java Agent.
 * <p>
 * Tracks:
 * <ul>
 *   <li>Total classes scanned</li>
 *   <li>Total classes successfully transformed</li>
 *   <li>Total transformation errors</li>
 *   <li>Total method invocations counted (COUNT mode)</li>
 * </ul>
 * All counters are thread-safe.
 *
 * @author JOJIN JOHN
 */
public final class AgentStats {

    private static final AtomicInteger classesScanned     = new AtomicInteger(0);
    private static final AtomicInteger classesTransformed = new AtomicInteger(0);
    private static final AtomicInteger transformErrors    = new AtomicInteger(0);
    private static final AtomicLong    methodInvocations  = new AtomicLong(0L);

    private AgentStats() {}

    /** Increments the classes-scanned counter. */
    public static void recordScan()              { classesScanned.incrementAndGet(); }

    /** Increments the classes-transformed counter. */
    public static void recordTransform()         { classesTransformed.incrementAndGet(); }

    /** Increments the error counter. */
    public static void recordError()             { transformErrors.incrementAndGet(); }

    /** Increments and returns the method invocation counter (used by COUNT mode). */
    public static long recordInvocation()        { return methodInvocations.incrementAndGet(); }

    /** @return number of classes scanned so far */
    public static int getClassesScanned()        { return classesScanned.get(); }

    /** @return number of classes successfully transformed */
    public static int getClassesTransformed()    { return classesTransformed.get(); }

    /** @return number of transformation errors */
    public static int getTransformErrors()       { return transformErrors.get(); }

    /** @return total recorded method invocations */
    public static long getMethodInvocations()    { return methodInvocations.get(); }

    /**
     * Prints a formatted summary of all collected statistics to stdout.
     */
    public static void printSummary() {
        System.out.printf(
            "[Sentinel] ─── Statistics ──────────────────────────────%n" +
            "[Sentinel]   Classes scanned    : %,d%n" +
            "[Sentinel]   Classes transformed: %,d%n" +
            "[Sentinel]   Transform errors   : %,d%n" +
            "[Sentinel]   Method invocations : %,d%n" +
            "[Sentinel] ─────────────────────────────────────────────%n",
            classesScanned.get(),
            classesTransformed.get(),
            transformErrors.get(),
            methodInvocations.get()
        );
    }
}
