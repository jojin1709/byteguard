package demo.target;

/**
 * Controlled demo service providing candidate methods for runtime instrumentation testing.
 *
 * @author JOJIN JOHN
 */
public class TargetService {

    /**
     * Target method for Sentinel bytecode instrumentation.
     *
     * @return original uninstrumented greeting message
     */
    public String message() {
        return "Original message";
    }

    /**
     * Control method that must NOT be modified by the agent.
     *
     * @param a first operand
     * @param b second operand
     * @return sum of a and b
     */
    public int add(int a, int b) {
        return a + b;
    }
}
