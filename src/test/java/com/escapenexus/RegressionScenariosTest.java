package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: door unlock regression, inventory duplicate guard, and timer stability across restarts.
// - Happy path: initial unlock succeeds, unique items add once, timer restarts maintain duration.
// - Negative cases: repeated unlock attempts fail; duplicate pickups are rejected.
// - Boundary cases: double restart leaves duration unchanged.
// - Invariants: duration never drops below configured value on repeated starts.
class RegressionScenariosTest {

    @Test
    @DisplayName("bugfix_doorUnlock_onlyOnce_whenSolveCalledTwice")
    void bugfix_doorUnlock_onlyOnce_whenSolveCalledTwice() {
        Item key = new Item(UUID.randomUUID(), "Vault Key", "Used to unlock door", true, true, ItemState.NEW);
        Door door = new Door("Vault Door", "Secures the vault", UUID.randomUUID(), UUID.randomUUID());
        door.setKeyRequired(key);

        assertTrue(door.unlock(key));
        assertFalse(door.unlock(key), "Door should not unlock twice");
        assertTrue(door.open());
    }

    @Test
    @DisplayName("bugfix_inventory_noDuplicateOnRapidPickup")
    void bugfix_inventory_noDuplicateOnRapidPickup() {
        Inventory inventory = new Inventory();
        Item tool = new Item("Diagnostic Tool", "Non-key tool", true, false, ItemState.USED);

        assertTrue(inventory.add(tool));
        assertFalse(inventory.add(tool), "Rapid duplicate pickup should fail");
        assertEquals(1, inventory.getItems().size());
    }

    @Test
    @DisplayName("bugfix_timer_pauseTwice_noNegativeTime")
    void bugfix_timer_pauseTwice_noNegativeTime() {
        Game game = GameFactory.createDefaultThreeRoomGame(Difficulty.MEDIUM);
        game.setTimeLimit(Duration.ofMinutes(15));
        game.start();
        game.end();
        game.end(); // second pause/end call
        assertEquals(Duration.ofMinutes(15), game.getTimeLimit());

        game.start();
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }
}
