package demo.app;

import demo.target.TargetService;

/**
 * Entry point application to demonstrate execution with and without the Sentinel Java Agent.
 * <p>
 * Run WITHOUT agent to see original output:
 * <pre>
 *   java -jar target/sentinel-agent.jar
 * </pre>
 * Run WITH agent (default REPLACE mode):
 * <pre>
 *   java -javaagent:target/sentinel-agent.jar -jar target/sentinel-agent.jar
 * </pre>
 * Run with TRACE mode:
 * <pre>
 *   java -javaagent:target/sentinel-agent.jar=mode=TRACE -jar target/sentinel-agent.jar
 * </pre>
 * Run with COUNT mode:
 * <pre>
 *   java -javaagent:target/sentinel-agent.jar=mode=COUNT -jar target/sentinel-agent.jar
 * </pre>
 * Run with custom target class and verbose logging:
 * <pre>
 *   java -javaagent:target/sentinel-agent.jar=target=demo/target/TargetService,method=message,mode=REPLACE,verbose=true -jar target/sentinel-agent.jar
 * </pre>
 *
 * @author JOJIN JOHN
 */
public final class DemoApplication {

    private DemoApplication() {
        // Utility main class
    }

    /**
     * Executes the demo application against {@link TargetService}.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        TargetService service = new TargetService();

        System.out.println("─────────────────────────────────────────");
        System.out.println("  Sentinel Java Agent — Demo Application  ");
        System.out.println("  by JOJIN JOHN                           ");
        System.out.println("─────────────────────────────────────────");
        System.out.println();

        System.out.println("[Demo] Calling message() x3:");
        for (int i = 1; i <= 3; i++) {
            System.out.println("  [" + i + "] " + service.message());
        }

        System.out.println();
        System.out.println("[Demo] Calling add(2, 3) — must always return 5:");
        System.out.println("  Result = " + service.add(2, 3));

        System.out.println();
        System.out.println("[Demo] Calling add(10, 20) — must always return 30:");
        System.out.println("  Result = " + service.add(10, 20));

        System.out.println();
        System.out.println("─────────────────────────────────────────");
    }
}
