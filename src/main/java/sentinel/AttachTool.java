package sentinel;

import java.io.File;

/**
 * CLI tool for dynamic attach — attaches the Sentinel agent to a <em>running</em> JVM process
 * without restarting it, using the JDK Attach API.
 *
 * <h3>Requirements</h3>
 * <ul>
 *   <li>Java 17 JDK (not JRE) — the Attach API is in the {@code jdk.attach} module</li>
 *   <li>The target process must be running on the same machine</li>
 *   <li>On Linux, {@code /proc} filesystem access is required</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   java --add-modules jdk.attach -jar sentinel-agent.jar --attach &lt;PID&gt; [agentArgs]
 *   java --add-modules jdk.attach -jar sentinel-agent.jar --attach 12345 mode=TRACE,verbose=true
 *   java --add-modules jdk.attach -jar sentinel-agent.jar --list
 * </pre>
 *
 * @author JOJIN JOHN
 */
public final class AttachTool {

    private AttachTool() {}

    /**
     * Main entry point for dynamic attach operations.
     *
     * @param args command-line arguments: {@code --attach <PID> [agentArgs]} or {@code --list}
     */
    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("--help")) {
            printUsage();
            return;
        }

        if (args[0].equals("--list")) {
            listJvms();
            return;
        }

        if (args[0].equals("--attach")) {
            if (args.length < 2) {
                System.err.println("[AttachTool] ERROR: --attach requires a PID argument");
                printUsage();
                System.exit(1);
            }
            String pid       = args[1];
            String agentArgs = args.length > 2 ? args[2] : null;
            attachToProcess(pid, agentArgs);
            return;
        }

        System.err.println("[AttachTool] Unknown command: " + args[0]);
        printUsage();
        System.exit(1);
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private static void attachToProcess(String pid, String agentArgs) {
        // Resolve the agent JAR path (same JAR this class is running from)
        String agentJar = resolveAgentJar();
        if (agentJar == null) {
            System.err.println("[AttachTool] Could not resolve sentinel-agent.jar path");
            System.exit(1);
        }

        System.out.println("[AttachTool] Attaching to PID " + pid + " ...");
        System.out.println("[AttachTool] Agent JAR: " + agentJar);
        System.out.println("[AttachTool] Agent args: " + (agentArgs != null ? agentArgs : "(none)"));

        try {
            // Use reflection so the code compiles without --add-modules at compile time
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Object vm = vmClass.getMethod("attach", String.class).invoke(null, pid);
            vmClass.getMethod("loadAgent", String.class, String.class)
                   .invoke(vm, agentJar, agentArgs);
            vmClass.getMethod("detach").invoke(vm);

            System.out.println("[AttachTool] ✓ Successfully attached to PID " + pid);
            if (agentArgs != null) {
                System.out.println("[AttachTool] ✓ Agent args applied: " + agentArgs);
            }

        } catch (ClassNotFoundException e) {
            System.err.println("[AttachTool] ERROR: Attach API not available.");
            System.err.println("[AttachTool] Run with: java --add-modules jdk.attach -jar sentinel-agent.jar --attach " + pid);
        } catch (Exception e) {
            System.err.println("[AttachTool] ERROR: Failed to attach to PID " + pid + ": " + e.getMessage());
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.err.println("[AttachTool] Cause: " + cause.getMessage());
            System.exit(1);
        }
    }

    private static void listJvms() {
        System.out.println("[AttachTool] Running JVM processes:");
        System.out.println("─────────────────────────────────────────────");
        try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Class<?> vmdClass = Class.forName("com.sun.tools.attach.VirtualMachineDescriptor");
            @SuppressWarnings("unchecked")
            java.util.List<Object> vms = (java.util.List<Object>) vmClass.getMethod("list").invoke(null);
            for (Object vmd : vms) {
                String id          = (String) vmdClass.getMethod("id").invoke(vmd);
                String displayName = (String) vmdClass.getMethod("displayName").invoke(vmd);
                System.out.printf("  PID %-8s  %s%n", id, displayName);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[AttachTool] ERROR: Attach API not available. Run with --add-modules jdk.attach");
        } catch (Exception e) {
            System.err.println("[AttachTool] ERROR: " + e.getMessage());
        }
        System.out.println("─────────────────────────────────────────────");
    }

    private static String resolveAgentJar() {
        try {
            File jar = new File(
                AttachTool.class.getProtectionDomain()
                               .getCodeSource()
                               .getLocation()
                               .toURI()
            );
            return jar.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("Sentinel Java Agent — Dynamic Attach Tool");
        System.out.println("by JOJIN JOHN");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java --add-modules jdk.attach -jar sentinel-agent.jar --list");
        System.out.println("  java --add-modules jdk.attach -jar sentinel-agent.jar --attach <PID>");
        System.out.println("  java --add-modules jdk.attach -jar sentinel-agent.jar --attach <PID> <agentArgs>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java --add-modules jdk.attach -jar sentinel-agent.jar --list");
        System.out.println("  java --add-modules jdk.attach -jar sentinel-agent.jar --attach 12345");
        System.out.println("  java --add-modules jdk.attach -jar sentinel-agent.jar --attach 12345 mode=TRACE,verbose=true");
        System.out.println();
        System.out.println("agentArgs format:  key=value pairs, semicolon-separated targets");
        System.out.println("  target=com/example/MyService   — target class (glob: com/example/**)");
        System.out.println("  method=process                 — method name (default: message)");
        System.out.println("  mode=TRACE|REPLACE|COUNT|NULL_CHECK");
        System.out.println("  verbose=true|false");
        System.out.println("  logfile=/path/to/agent.log");
        System.out.println();
    }
}
