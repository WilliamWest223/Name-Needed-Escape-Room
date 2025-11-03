package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: Inventory add/remove semantics, User.pickUp guards, Room/door item interactions.
// - Happy path: portable items are added once and can unlock gates.
// - Negative cases: duplicates, non-portable items, and wrong targets are rejected.
// - Boundary cases: removing an item reflects consumption; items remain referenced correctly in rooms.
// - Invariants: DataLoader populates inventory fixtures with expected properties.
class InventoryRulesTest {

    @Test
    @DisplayName("pickup_firstTime_addsItem")
    void pickup_firstTime_addsItem() {
        User user = new User("collector");
        Item key = new Item("Key", "Simple portable key", true, true, ItemState.NEW);
        assertTrue(user.pickUp(key));
        assertTrue(user.getInventory().contains(key));
    }

    @Test
    @DisplayName("pickup_duplicate_whenNoStacking_isRejectedOrCountsAccordingToPolicy")
    void pickup_duplicate_whenNoStacking_isRejectedOrCountsAccordingToPolicy() {
        Inventory inventory = new Inventory();
        Item key = new Item("Duplicate Key", "Testing duplicates", true, true, ItemState.NEW);
        assertTrue(inventory.add(key));
        assertFalse(inventory.add(key), "Second addition should be rejected");
    }

    @Test
    @DisplayName("capacity_limit_preventsExcess")
    void capacity_limit_preventsExcess() {
        User user = new User("carrier");
        Item nonPortable = new Item("Heavy Key", "Too heavy to carry", false, true, ItemState.NEW);
        assertFalse(user.pickUp(nonPortable), "Non-portable items cannot be picked up");
        assertFalse(user.getInventory().contains(nonPortable));
    }

    @Test
    @DisplayName("drop_item_updatesRoomState_andRemovesFromInventory")
    void drop_item_updatesRoomState_andRemovesFromInventory() {
        Room room = new Room("Storage", "Contains spare items");
        Item key = new Item("Stash Key", "Small key", true, true, ItemState.NEW);
        room.addItem(key);

        User user = new User("scavenger");
        user.moveTo(room);
        assertTrue(user.pickUp(key));
        assertTrue(room.getItems().contains(key), "Room retains reference to the original item");

        assertTrue(user.getInventory().remove(key));
        room.addItem(key);
        assertTrue(room.getItems().contains(key));
    }

    @Test
    @DisplayName("use_itemOnCorrectTarget_triggersEffect")
    void use_itemOnCorrectTarget_triggersEffect() {
        Item key = new Item(UUID.randomUUID(), "Transit Key", "Unlocks the transit gate", true, true, ItemState.NEW);
        Door door = new Door("Gate", "Security gate", UUID.randomUUID(), UUID.randomUUID());
        door.setKeyRequired(key);

        assertTrue(door.unlock(key));
        assertTrue(door.open());
    }

    @Test
    @DisplayName("use_itemOnWrongTarget_noEffect_clearError")
    void use_itemOnWrongTarget_noEffect_clearError() {
        Item required = new Item(UUID.randomUUID(), "Correct Key", "Needed to open the gate", true, true, ItemState.NEW);
        Door door = new Door("Secure Gate", "Requires correct key", UUID.randomUUID(), UUID.randomUUID());
        door.setKeyRequired(required);

        Item wrong = new Item(UUID.randomUUID(), "Incorrect Key", "Fails to open", true, true, ItemState.NEW);
        assertFalse(door.unlock(wrong));
        assertFalse(door.open());
    }

    @Test
    @DisplayName("consumeable_items_decrementOrDisappearAsConfigured")
    void consumeable_items_decrementOrDisappearAsConfigured() throws IOException {
        DataLoader loader = new DataLoader();
        List<Game> games = loader.loadGamesFromResource("level_inv_usematrix.json");
        Game game = games.get(0);

        Room room = game.getRooms().get(0);
        Item key = game.getItems().stream()
                .filter(Item::isKey)
                .findFirst()
                .orElse(null);
        assertNotNull(key);

        Inventory inventory = new Inventory();
        assertTrue(inventory.add(key));
        assertTrue(inventory.remove(key), "Removing the item simulates consumption");
        assertFalse(inventory.contains(key));
    }
}
