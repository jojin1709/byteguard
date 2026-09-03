package demo.app;

import demo.target.TargetService;

/**
 * Entry point application to demonstrate execution with and without the Sentinel Java Agent.
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
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        TargetService service = new TargetService();

        System.out.println(service.message());
        System.out.println(service.add(2, 3));
    }
}
