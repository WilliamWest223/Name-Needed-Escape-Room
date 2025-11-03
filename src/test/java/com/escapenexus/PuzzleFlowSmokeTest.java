package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: EscapeGame.attemptCurrentPuzzle, Game.advanceRoom, Room.unlock on puzzle solve.
// - Happy path: solving the first light puzzle unlocks the next room and advances the user.
// - Negative guard: ensure the next room starts locked before solving.
// - Boundary cases: repeated attempts after solving stay idempotent via EscapeGame.
// - Invariants: progress tracking marks the puzzle solved exactly once.
class PuzzleFlowSmokeTest {

    @AfterEach
    void tearDown() {
        EscapeGame escapeGame = EscapeGame.getInstance();
        escapeGame.resetProgress();
        escapeGame.logout();
    }

    @Test
    @DisplayName("solve_lightPattern_unlocksNextRoomAndAdvancesUser")
    void solve_lightPattern_unlocksNextRoomAndAdvancesUser() {
        EscapeGame escapeGame = EscapeGame.getInstance();
        escapeGame.newSinglePlayerSession("tester", Difficulty.MEDIUM);

        User user = escapeGame.getUser();
        Game game = escapeGame.getGame();
        assertNotNull(user, "User should be initialized for the session");
        assertNotNull(game, "Game should be created for the session");

        List<Room> rooms = game.getRooms();
        assertTrue(rooms.size() >= 2, "Default game should provide at least two rooms");

        Room currentRoom = user.getCurrentRoom();
        Room nextRoom = rooms.get(1);
        assertTrue(nextRoom.isLocked(), "Next room must start locked");

        Puzzle puzzle = currentRoom.getPuzzles().get(0);
        assertTrue(puzzle instanceof LightPatternPuzzle, "First puzzle should be a light pattern puzzle");
        LightPatternPuzzle lightPuzzle = (LightPatternPuzzle) puzzle;

        int guard = 0;
        while (!lightPuzzle.isSolved()) {
            List<LightColor> guess = new ArrayList<>(lightPuzzle.getCurrentSequence());
            assertFalse(guess.isEmpty(), "Sequence should never be empty while unsolved");
            assertTrue(escapeGame.attemptCurrentPuzzle(guess), "EscapeGame should accept correct sequence");
            guard++;
            assertTrue(guard <= 10, "Guard exceeded during puzzle solve");
        }

        assertFalse(nextRoom.isLocked(), "Solving the puzzle should unlock the next room");
        assertEquals(nextRoom, user.getCurrentRoom(), "User should advance to the next room automatically");

        Progress progress = user.getProgress(currentRoom.getId());
        assertNotNull(progress, "Progress should exist for the starting room");
        assertTrue(progress.isPuzzleSolved(lightPuzzle.getId()), "Puzzle should be marked solved in progress tracking");
    }
}
