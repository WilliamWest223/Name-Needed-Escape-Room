package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: Door.unlock/open state changes, LightPatternPuzzle side-effects via provided key.
// - Happy path: solving the puzzle yields the key, unlocking the door, and allowing opening exactly once.
// - Negative cases: wrong key or premature open attempts keep the door locked/closed.
// - Boundary cases: repeated solve/unlock attempts are idempotent and do not throw.
// - Invariants: unlocked doors remain unlocked after close; solved puzzle continues returning true.
class DoorUnlockFromPuzzleTest {

    private LightPatternPuzzle puzzle;
    private Door door;
    private Item accessKey;

    @BeforeEach
    void setUp() {
        accessKey = new Item(
                UUID.fromString("feedbeef-0000-0000-0000-000000000001"),
                "Access Key",
                "Unlocks the transit door.",
                true,
                true,
                ItemState.NEW
        );
        door = new Door(
                UUID.fromString("feedbeef-0000-0000-0000-000000000002"),
                "Transit Door",
                "Separates the cryo bay and transit hall.",
                UUID.fromString("feedbeef-0000-0000-0000-000000000010"),
                UUID.fromString("feedbeef-0000-0000-0000-000000000020")
        );
        door.setKeyRequired(accessKey);

        puzzle = new LightPatternPuzzle(
                UUID.fromString("feedbeef-0000-0000-0000-000000000003"),
                "Reboot Sequence",
                "Replay the flashing lights to receive the key.",
                24680L
        );
        puzzle.setKeyProvided(accessKey);
    }

    @Test
    @DisplayName("solve_doorBecomesUnlocked_andIsIdempotent")
    void solve_doorBecomesUnlocked_andIsIdempotent() {
        solvePuzzle(puzzle);
        assertTrue(puzzle.isSolved());

        assertTrue(door.unlock(puzzle.getKeyProvided()), "Puzzle key should unlock the door once");
        assertFalse(door.isLocked());

        assertTrue(door.open(), "Unlocked door should open");
        assertTrue(door.isOpen());

        assertFalse(door.unlock(puzzle.getKeyProvided()), "Door remains unlocked after the first unlock");
        door.close();
        assertFalse(door.isLocked(), "Closing should not re-lock an unlocked door");
        assertFalse(door.isOpen());
    }

    @Test
    @DisplayName("unsolved_preventsTraversal")
    void unsolved_preventsTraversal() {
        assertFalse(door.open(), "Door must not open while locked");
        assertTrue(door.isLocked());

        solvePuzzle(puzzle);
        assertTrue(door.unlock(puzzle.getKeyProvided()), "Solved puzzle should provide matching key");
        assertTrue(door.open());
        assertTrue(door.isOpen());
    }

    @Test
    @DisplayName("doubleSolve_doesNotDoubleUnlockOrThrow")
    void doubleSolve_doesNotDoubleUnlockOrThrow() {
        solvePuzzle(puzzle);
        assertTrue(door.unlock(puzzle.getKeyProvided()));
        assertFalse(door.isLocked());

        // Attempting to solve again should be harmless.
        List<LightColor> solvedSequence = new ArrayList<>(puzzle.getCurrentSequence());
        assertTrue(puzzle.attempt(solvedSequence), "Solved puzzle should continue returning true on attempts");

        assertFalse(door.unlock(puzzle.getKeyProvided()), "Unlock remains idempotent after repeated solves");
        assertFalse(door.isLocked());
    }

    @Test
    @DisplayName("attempt_withWrongKey_keepsDoorLocked")
    void attempt_withWrongKey_keepsDoorLocked() {
        Item wrongKey = new Item(
                UUID.fromString("feedbeef-0000-0000-0000-000000000099"),
                "Wrong Key",
                "Does not match the door.",
                true,
                true,
                ItemState.NEW
        );
        assertFalse(door.unlock(wrongKey));
        assertTrue(door.isLocked());

        solvePuzzle(puzzle);
        assertTrue(door.unlock(accessKey));
        assertFalse(door.isLocked());
    }

    private void solvePuzzle(LightPatternPuzzle target) {
        int guard = 0;
        while (!target.isSolved()) {
            List<LightColor> guess = new ArrayList<>(target.getCurrentSequence());
            assertFalse(guess.isEmpty());
            assertTrue(target.attempt(guess));
            guard++;
            assertTrue(guard <= 10, "Guard exceeded while solving puzzle");
        }
    }
}
