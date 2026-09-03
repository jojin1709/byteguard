package demo.target;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TargetService Unit Tests")
class TargetServiceTest {

    @Test
    @DisplayName("message() should return default uninstrumented string")
    void testOriginalMessage() {
        TargetService service = new TargetService();
        assertEquals("Original message", service.message(), "Uninstrumented message must be 'Original message'");
    }

    @Test
    @DisplayName("add() should correctly compute arithmetic sum")
    void testAddMethod() {
        TargetService service = new TargetService();
        assertEquals(5, service.add(2, 3));
        assertEquals(0, service.add(-5, 5));
        assertEquals(100, service.add(60, 40));
    }
}
