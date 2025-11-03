package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Test Plan:
// - Coverage targets: Door unlocking policies, Room.unlock contract, DataLoader integration for navigation fixtures.
// - Happy path: loaded rooms reflect configured lock state; matching keys unlock doors and rooms.
// - Negative cases: wrong or null keys fail gracefully; locked resources remain locked.
// - Boundary cases: repeated unlock attempts are idempotent; empty rooms load without errors.
// - Invariants: DataLoader links items correctly for navigation JSON fixtures.
class TraversalRulesTest {

    @Test
    @DisplayName("load_nav_minimal_json_setsLockedStates")
    void load_nav_minimal_json_setsLockedStates() throws IOException {
        DataLoader loader = new DataLoader();
        List<Game> games = loader.loadGamesFromResource("level_nav_minimal.json");
        assertEquals(1, games.size());

        Game game = games.get(0);
        assertEquals(2, game.getRooms().size());
        Room start = game.getRooms().get(0);
        Room target = game.getRooms().get(1);

        assertFalse(start.isLocked());
        assertTrue(target.isLocked());

        Puzzle puzzle = start.getPuzzles().get(0);
        Item providedKey = puzzle.getKeyProvided();
        assertEquals(target.getKeyRequired().getId(), providedKey.getId());

        assertTrue(target.unlock(providedKey));
        assertFalse(target.isLocked());
        assertFalse(target.unlock(providedKey), "Unlock remains idempotent after success");
    }

    @Test
    @DisplayName("load_nav_locked_json_retainsPermanentLocks")
    void load_nav_locked_json_retainsPermanentLocks() throws IOException {
        DataLoader loader = new DataLoader();
        List<Game> games = loader.loadGamesFromResource("level_nav_locked.json");
        assertEquals(1, games.size());

        Room sealed = games.get(0).getRooms().get(1);
        assertTrue(sealed.isLocked());
        assertFalse(sealed.unlock(new Item("Ghost Key", "Invalid", true, true, ItemState.NEW)));
        assertTrue(sealed.isLocked());
    }

    @ParameterizedTest(name = "door_policy keyMatch={2}")
    @MethodSource("doorPolicies")
    void door_policy_keyMatching_behavesAsExpected(Item requiredKey, Item attemptKey, boolean expected) {
        Door door = new Door(
                UUID.randomUUID(),
                "Test Door",
                "Door under test",
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        door.setKeyRequired(requiredKey);

        boolean unlockResult = door.unlock(attemptKey);
        assertEquals(expected, unlockResult);

        if (expected) {
            assertTrue(door.open());
            assertFalse(door.unlock(attemptKey), "Second unlock should fail once door is open");
        } else {
            assertFalse(door.open(), "Door stays closed for non-matching keys");
        }
    }

    private static Stream<Arguments> doorPolicies() {
        Item matchingKey = new Item(
                UUID.fromString("50000000-0000-0000-0000-000000000000"),
                "Matching Key",
                "Opens the door.",
                true,
                true,
                ItemState.NEW
        );
        Item nonMatchingKey = new Item(
                UUID.fromString("60000000-0000-0000-0000-000000000000"),
                "Wrong Key",
                "Should not open the door.",
                true,
                true,
                ItemState.NEW
        );

        return Stream.of(
                Arguments.of(matchingKey, matchingKey, true),
                Arguments.of(matchingKey, nonMatchingKey, false),
                Arguments.of(matchingKey, null, false)
        );
    }
}
