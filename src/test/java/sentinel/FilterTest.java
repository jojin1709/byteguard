package sentinel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Filter Utility Tests")
class FilterTest {

    @Test
    @DisplayName("normalize() handles null by returning empty string")
    void testNormalizeNull() {
        assertEquals("", Filter.normalize(null));
    }

    @Test
    @DisplayName("normalize() strips leading and trailing whitespace")
    void testNormalizeTrimming() {
        assertEquals("sentinel", Filter.normalize("  sentinel  "));
        assertEquals("demo/target/TargetService", Filter.normalize("\t demo/target/TargetService \n"));
    }

    @Test
    @DisplayName("isValidClassName() checks for non-null non-blank names")
    void testIsValidClassName() {
        assertTrue(Filter.isValidClassName("demo/target/TargetService"));
        assertFalse(Filter.isValidClassName(null));
        assertFalse(Filter.isValidClassName(""));
        assertFalse(Filter.isValidClassName("   "));
    }
}
