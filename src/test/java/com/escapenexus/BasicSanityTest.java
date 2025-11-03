package com.escapenexus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BasicSanityTest {

    @Test
    public void addsNumbers() {
        assertEquals(2, 1 + 1, "1 + 1 should equal 2");
    }

    @Test
    public void alwaysTrue() {
        assertTrue(true);
    }
}

