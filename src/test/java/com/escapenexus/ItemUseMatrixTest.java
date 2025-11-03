package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Test Plan:
// - Coverage targets: Room.unlock and Door.unlock interactions with varied items.
// - Happy path: correct item unlocks the intended target.
// - Negative cases: mismatched items leave targets locked.
// - Boundary cases: null items are rejected; subsequent unlock calls are idempotent.
// - Invariants: key flag drives unlock success.
class ItemUseMatrixTest {

    @ParameterizedTest(name = "item-use target={0} expected={2}")
    @MethodSource("itemMatrix")
    void use_itemOnCorrectTarget_triggersExpectedOutcome(Object target, Item attempt, boolean expected) {
        if (target instanceof Room room) {
            room.setLocked(true);
            room.setKeyRequired(attempt);
            boolean result = room.unlock(attempt);
            assertTrue(result);
            assertFalse(room.isLocked());
            assertFalse(room.unlock(attempt), "Repeated unlock remains idempotent");
        } else if (target instanceof Door door) {
            boolean result = door.unlock(attempt);
            if (expected) {
                assertTrue(result);
                assertTrue(door.open());
            } else {
                assertFalse(result);
                assertFalse(door.open());
            }
        }
    }

    @Test
    @DisplayName("use_itemOnWrongTarget_noEffect_clearError")
    void use_itemOnWrongTarget_noEffect_clearError() {
        Item key = new Item(UUID.randomUUID(), "Matrix Key", "Designed for room unlock", true, true, ItemState.NEW);
        Item tool = new Item(UUID.randomUUID(), "Matrix Tool", "Non-key tool", true, false, ItemState.USED);

        Room room = new Room("Matrix Room", "Target room");
        room.setLocked(true);
        room.setKeyRequired(key);

        assertFalse(room.unlock(tool));
        assertTrue(room.isLocked());
    }

    private static Stream<Arguments> itemMatrix() {
        Item roomKey = new Item(UUID.randomUUID(), "Room Key", "Unlocks the room", true, true, ItemState.NEW);
        Item doorKey = new Item(UUID.randomUUID(), "Door Key", "Opens the door", true, true, ItemState.NEW);
        Item doorKeyFailure = new Item(UUID.randomUUID(), "Door Key Failure", "Actual key for failure door", true, true, ItemState.NEW);
        Item wrongKey = new Item(UUID.randomUUID(), "Wrong Key", "Does not open the door", true, true, ItemState.NEW);

        Room room = new Room("Matrix Target Room", "Requires matching key");
        room.setKeyRequired(roomKey);

        Door door = new Door("Matrix Door", "Requires specific key", UUID.randomUUID(), UUID.randomUUID());
        door.setKeyRequired(doorKey);

        Door doorFailure = new Door("Matrix Door Fail", "Requires specific key", UUID.randomUUID(), UUID.randomUUID());
        doorFailure.setKeyRequired(doorKeyFailure);

        Door doorWithNull = new Door("Matrix Door Null", "Requires key but receives null", UUID.randomUUID(), UUID.randomUUID());
        doorWithNull.setKeyRequired(new Item(UUID.randomUUID(), "Another Key", "Placeholder", true, true, ItemState.NEW));

        return Stream.of(
                Arguments.of(room, roomKey, true),
                Arguments.of(door, doorKey, true),
                Arguments.of(doorFailure, wrongKey, false),
                Arguments.of(doorWithNull, null, false)
        );
    }
}
