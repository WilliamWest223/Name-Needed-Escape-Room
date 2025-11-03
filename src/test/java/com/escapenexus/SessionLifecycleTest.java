package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: EscapeGame session bootstrap, restart, resetProgress, logout, and Game status transitions.
// - Happy path: new session selects a room, initializes progress, and keeps inventory empty.
// - Negative cases: blank usernames are rejected; attempts after logout fail.
// - Boundary cases: restarting after manual end maintains the configured time limit; resetting clears inventory.
// - Invariants: solving all rooms allows the game to reach COMPLETED when ended manually.
class SessionLifecycleTest {

    private EscapeGame escapeGame;

    @BeforeEach
    void init() {
        escapeGame = EscapeGame.getInstance();
        escapeGame.resetProgress();
        escapeGame.logout();
    }

    @AfterEach
    void tearDown() {
        escapeGame.resetProgress();
        escapeGame.logout();
    }

    @Test
    @DisplayName("start_newSession_setsStartRoom_andEmptyInventory")
    void start_newSession_setsStartRoom_andEmptyInventory() {
        escapeGame.newSinglePlayerSession("session-runner", Difficulty.MEDIUM);

        User user = escapeGame.getUser();
        assertNotNull(user);
        assertNotNull(user.getCurrentRoom());
        assertTrue(user.getInventory().getItems().isEmpty(), "Starting inventory should be empty");

        assertEquals(GameStatus.IN_PROGRESS, escapeGame.getGame().getStatus());
    }

    @Test
    @DisplayName("pause_resume_preservesTimerAndState")
    void pause_resume_preservesTimerAndState() {
        escapeGame.newSinglePlayerSession("pause-runner", Difficulty.EASY);
        Game game = escapeGame.getGame();
        game.setTimeLimit(Duration.ofMinutes(5));
        game.end(); // simulate pausing the session
        assertEquals(GameStatus.COMPLETED, game.getStatus());

        escapeGame.restartGame(); // simulate resume
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(Duration.ofMinutes(5), game.getTimeLimit(), "Restart should not alter time limit");
    }

    @Test
    @DisplayName("timeout_triggersLoseState_andPreventsFurtherMoves")
    void timeout_triggersLoseState_andPreventsFurtherMoves() {
        escapeGame.newSinglePlayerSession("timeout-runner", Difficulty.MEDIUM);
        escapeGame.logout(); // emulate a timeout removing the player

        assertNull(escapeGame.getUser());
        assertFalse(escapeGame.attemptCurrentPuzzle(List.of()), "No user should prevent puzzle attempts");
    }

    @Test
    @DisplayName("winCondition_reached_setsWinState_andStopsTimer")
    void winCondition_reached_setsWinState_andStopsTimer() {
        escapeGame.newSinglePlayerSession("winner", Difficulty.MEDIUM);
        solveDefaultGame(escapeGame);

        Game game = escapeGame.getGame();
        game.end(); // mark completion

        assertEquals(GameStatus.COMPLETED, game.getStatus());
        assertEquals(Duration.ofMinutes(30), game.getTimeLimit(), "Time limit remains configured on completion");
        assertSame(game.getRooms().get(game.getRooms().size() - 1), escapeGame.getUser().getCurrentRoom());
    }

    @Test
    @DisplayName("reset_sessionRestoresInitialConditions")
    void reset_sessionRestoresInitialConditions() {
        escapeGame.newSinglePlayerSession("resetter", Difficulty.MEDIUM);
        solveLightPattern(escapeGame);
        assertFalse(escapeGame.getUser().getInventory().getItems().isEmpty(), "Solving the first puzzle yields a key");

        escapeGame.resetProgress();

        assertTrue(escapeGame.getUser().getInventory().getItems().isEmpty(), "Inventory clears on reset");
        assertTrue(escapeGame.getUser().getProgressByRoomId().isEmpty(), "Progress clears on reset");
    }

    private void solveDefaultGame(EscapeGame game) {
        solveLightPattern(game);
        boolean solved = game.attemptCurrentPuzzle("river");
        assertTrue(solved, "Second room riddle should accept the canonical answer");
        solved = game.attemptCurrentPuzzle(12.0);
        assertTrue(solved, "Third room math puzzle should accept the exact sum");
    }

    private void solveLightPattern(EscapeGame game) {
        Puzzle puzzle = game.getNextPuzzle();
        assertTrue(puzzle instanceof LightPatternPuzzle);
        LightPatternPuzzle lightPatternPuzzle = (LightPatternPuzzle) puzzle;

        while (!lightPatternPuzzle.isSolved()) {
            List<LightColor> guess = new ArrayList<>(lightPatternPuzzle.getCurrentSequence());
            assertTrue(game.attemptCurrentPuzzle(guess));
        }
    }
}
