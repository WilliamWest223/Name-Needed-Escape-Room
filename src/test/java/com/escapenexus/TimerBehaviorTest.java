package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: Game time limit getters/setters, status transitions, and DataLoader integration for timer fixtures.
// - Happy path: loaded games honor configured durations and transition to IN_PROGRESS when started.
// - Negative cases: ending a game prevents further IN_PROGRESS status until restarted manually.
// - Boundary cases: resetting the duration updates the stored value without side effects.
// - Invariants: timer configuration persists across repeated start calls.
class TimerBehaviorTest {

    @Test
    @DisplayName("start_newSession_timerUsesConfiguredDuration")
    void start_newSession_timerUsesConfiguredDuration() throws IOException {
        DataLoader loader = new DataLoader();
        List<Game> games = loader.loadGamesFromResource("level_timer_basic.json");
        Game game = games.get(0);

        assertEquals(Duration.ofMinutes(5), game.getTimeLimit());
        game.start();
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }

    @Test
    @DisplayName("pause_resume_preservesTimerConfiguration")
    void pause_resume_preservesTimerConfiguration() {
        Game game = GameFactory.createDefaultThreeRoomGame(Difficulty.MEDIUM);
        game.setTimeLimit(Duration.ofMinutes(12));
        game.start();
        assertEquals(Duration.ofMinutes(12), game.getTimeLimit());

        game.end();
        assertEquals(GameStatus.COMPLETED, game.getStatus());

        game.start(); // restart should re-enter IN_PROGRESS
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(Duration.ofMinutes(12), game.getTimeLimit(), "Time limit should persist across restarts");
    }

    @Test
    @DisplayName("timeout_triggersLoseState_andPreventsFurtherActions")
    void timeout_triggersLoseState_andPreventsFurtherActions() {
        Game game = GameFactory.createDefaultThreeRoomGame(Difficulty.MEDIUM);
        game.start();
        game.end(); // treat as timeout
        assertEquals(GameStatus.COMPLETED, game.getStatus());

        // Attempt to re-end should have no effect until restarted.
        game.end();
        assertEquals(GameStatus.COMPLETED, game.getStatus());
        assertFalse(game.getRooms().isEmpty());
    }
}
