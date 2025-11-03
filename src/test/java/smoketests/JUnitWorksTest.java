// File: src/test/java/smoketests/JUnitWorksTest.java
package smoketests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JUnitWorksTest {
    @Test void sanity() { assertEquals(2, 1 + 1); }
}

