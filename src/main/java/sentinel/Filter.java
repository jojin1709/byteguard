package sentinel;

/**
 * Generic utility functions for the Sentinel Java Agent.
 * <p>
 * Provides safe string normalization and class name validation helpers
 * without proprietary cryptography or licensing-specific operations.
 *
 * @author JOJIN JOHN
 */
public final class Filter {

    private Filter() {
        // Utility class; prevent instantiation
    }

    /**
     * Normalizes an input string by trimming whitespace.
     *
     * @param value the raw string to normalize
     * @return the trimmed string, or an empty string if {@code value} is {@code null}
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    /**
     * Validates whether a given class name conforms to standard non-empty internal representation.
     *
     * @param internalName the internal JVM class name (e.g., {@code demo/target/TargetService})
     * @return {@code true} if the name is non-null and not blank, {@code false} otherwise
     */
    public static boolean isValidClassName(String internalName) {
        return internalName != null && !internalName.isBlank();
    }
}
