package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: EscapeGame multi-room advancement, Progress bookkeeping, and user relocation.
// - Happy path: solving puzzles in order unlocks successive rooms and ends in the final chamber.
// - Negative cases: wrong puzzle input keeps the next room locked and the user stationary.
// - Boundary cases: resetting progress acts as a soft checkpoint; timeout/logouts block further actions.
// - Invariants: final room remains current after clearing all puzzles.
class GameFlowEndToEndTest {

    private EscapeGame escapeGame;

    @BeforeEach
    void setUp() {
        escapeGame = EscapeGame.getInstance();
        escapeGame.resetProgress();
        escapeGame.logout();
        escapeGame.newSinglePlayerSession("flow-runner", Difficulty.MEDIUM);
    }

    @AfterEach
    void tearDown() {
        escapeGame.resetProgress();
        escapeGame.logout();
    }

    @Test
    @DisplayName("journey_solveGate_moveAcrossRooms_reachWin")
    void journey_solveGate_moveAcrossRooms_reachWin() {
        solveLightPattern();
        assertEquals("Transit Hall", escapeGame.getUser().getCurrentRoom().getName());

        assertTrue(escapeGame.attemptCurrentPuzzle("river"));
        assertEquals("Core Vault", escapeGame.getUser().getCurrentRoom().getName());

        assertTrue(escapeGame.attemptCurrentPuzzle(12.0));
        Room finalRoom = escapeGame.getUser().getCurrentRoom();
        assertSame(finalRoom, escapeGame.getGame().getRooms().get(2));
    }

    @Test
    @DisplayName("journey_wrongOrder_keepsLocked_thenCorrectOrder_unlocks")
    void journey_wrongOrder_keepsLocked_thenCorrectOrder_unlocks() {
        solveLightPattern();
        Room finalRoom = escapeGame.getGame().getRooms().get(2);
        assertTrue(finalRoom.isLocked());

        assertFalse(escapeGame.attemptCurrentPuzzle("wrong"));
        assertTrue(finalRoom.isLocked(), "Final room stays locked after incorrect answer");
        assertEquals("Transit Hall", escapeGame.getUser().getCurrentRoom().getName());

        assertTrue(escapeGame.attemptCurrentPuzzle("river"));
        assertFalse(finalRoom.isLocked());
        assertEquals("Core Vault", escapeGame.getUser().getCurrentRoom().getName());
    }

    @Test
    @DisplayName("journey_checkpoint_restore_resumesFromCheckpoint")
    void journey_checkpoint_restore_resumesFromCheckpoint() {
        solveLightPattern();
        User user = escapeGame.getUser();
        Room current = user.getCurrentRoom();
        Progress saved = user.getProgress(current.getId());
        assertNotNull(saved);

        escapeGame.resetProgress();
        user.putProgress(current.getId(), saved);

        assertFalse(user.getProgressByRoomId().isEmpty());
        assertEquals(current, user.getCurrentRoom());
    }

    @Test
    @DisplayName("journey_timeout_midway_transitionsToLose_andBlocksFurtherActions")
    void journey_timeout_midway_transitionsToLose_andBlocksFurtherActions() {
        solveLightPattern();
        escapeGame.logout(); // emulate timeout removing player
        assertFalse(escapeGame.attemptCurrentPuzzle("river"));
        assertNull(escapeGame.getUser());
    }

    private void solveLightPattern() {
        Puzzle puzzle = escapeGame.getNextPuzzle();
        assertTrue(puzzle instanceof LightPatternPuzzle);
        LightPatternPuzzle lightPatternPuzzle = (LightPatternPuzzle) puzzle;

        while (!lightPatternPuzzle.isSolved()) {
            List<LightColor> guess = new ArrayList<>(lightPatternPuzzle.getCurrentSequence());
            assertTrue(escapeGame.attemptCurrentPuzzle(guess));
        }
    }
}
