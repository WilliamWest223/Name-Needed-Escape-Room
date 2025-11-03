package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Test Plan:
// - Coverage targets: LightPatternPuzzle.attempt(), getCurrentSequence(), giveHint() fallback/override, solved gating.
// - Happy path: correct sequences across all rounds solve the puzzle and expose the provided key pattern once.
// - Negative cases: wrong colors, mismatched lengths, null/empty/invalid inputs leave the puzzle unsolved.
// - Boundary cases: accept String inputs case-insensitively, allow repeat attempts post-solve without regressions.
// - Reset/invariants: once solved, attempts remain true without altering the solved state or shrinking the sequence.
class LightPatternPuzzleTest {

    private LightPatternPuzzle puzzle;

    @BeforeEach
    void setUp() {
        puzzle = new LightPatternPuzzle(
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                "Reboot Sequence",
                "Replay the flashing lights.",
                12345L
        );
    }

    @Test
    @DisplayName("evaluate_exactSequence_unlocksDoor progression across all rounds")
    void evaluate_exactSequence_unlocksDoor() {
        int roundsCompleted = 0;
        while (!puzzle.isSolved()) {
            List<LightColor> guess = copyCurrentSequence();
            assertEquals(roundsCompleted + 1, guess.size(), "Sequence length should match round index");
            assertTrue(puzzle.attempt(guess), "Round " + roundsCompleted + " should accept the exact pattern");
            roundsCompleted++;
        }
        assertEquals(copyCurrentSequence().size(), roundsCompleted, "Final sequence length tracks completed rounds");
        assertTrue(puzzle.isSolved());
    }

    @Test
    @DisplayName("evaluate_wrongAttempt_keepsLocked")
    void evaluate_wrongAttempt_keepsLocked() {
        List<LightColor> wrongGuess = copyCurrentSequence();
        wrongGuess.set(0, wrongGuess.get(0) == LightColor.RED ? LightColor.GREEN : LightColor.RED);

        assertFalse(puzzle.attempt(wrongGuess));
        assertFalse(puzzle.isSolved());
        assertEquals(1, copyCurrentSequence().size(), "Puzzle should stay on the initial round after a failure");
    }

    @Test
    @DisplayName("evaluate_multipleWrong_thenCorrect_advancesSingleRound")
    void evaluate_multipleWrong_thenCorrect_advancesSingleRound() {
        List<LightColor> firstRound = copyCurrentSequence();
        int initialLength = firstRound.size();

        List<LightColor> wrong = new ArrayList<>(firstRound);
        wrong.set(0, wrong.get(0) == LightColor.BLUE ? LightColor.RED : LightColor.BLUE);
        assertFalse(puzzle.attempt(wrong));
        assertFalse(puzzle.attempt(wrong));
        assertEquals(initialLength, copyCurrentSequence().size(), "Failed attempts must not advance the round");

        assertTrue(puzzle.attempt(firstRound));
        assertFalse(puzzle.isSolved(), "Puzzle should not be solved after the first successful round");
        assertEquals(initialLength + 1, copyCurrentSequence().size(), "Successful attempt advances to the next round");
    }

    @Test
    @DisplayName("evaluate_boundaryInputs_acceptsStringSequenceCaseInsensitive")
    void evaluate_boundaryInputs_acceptsStringSequenceCaseInsensitive() {
        List<String> guess = puzzle.getCurrentSequence().stream()
                .map(color -> color.name().toLowerCase())
                .collect(Collectors.toList());

        assertTrue(puzzle.attempt(guess));
        assertFalse(puzzle.isSolved(), "Solving the first round should not mark the puzzle as solved");
    }

    @ParameterizedTest(name = "evaluate_invalidInput_{index}")
    @MethodSource("invalidInputs")
    void evaluate_invalidInput_returnsFalse(Object attempt) {
        assertFalse(puzzle.attempt(attempt));
        assertFalse(puzzle.isSolved());
    }

    @Test
    @DisplayName("evaluate_afterSolved_isIdempotent")
    void evaluate_afterSolved_isIdempotent() {
        solveCompletely();
        assertTrue(puzzle.isSolved());

        assertTrue(puzzle.attempt(List.of(LightColor.RED)), "Solved puzzle should accept redundant attempts");
        assertTrue(puzzle.isSolved());
    }

    @Test
    @DisplayName("giveHint_usesCustomHintsWhenAvailable")
    void giveHint_usesCustomHintsWhenAvailable() {
        puzzle.addHint("Follow the rhythm.");
        puzzle.addHint("Repeat back exactly.");

        assertEquals("Follow the rhythm.", puzzle.giveHint());
        assertEquals("Repeat back exactly.", puzzle.giveHint());
    }

    @Test
    @DisplayName("giveHint_withoutCustom_returnsDefaultByRound")
    void giveHint_withoutCustom_returnsDefaultByRound() {
        assertEquals("Count the flashes and replay in order.", puzzle.giveHint());
        solveRound();
        assertEquals("Count the flashes and replay in order.", puzzle.giveHint(), "Early rounds reuse the basic guidance");
        solveRound();
        assertEquals("Chunk colors into small groups.", puzzle.giveHint(), "Mid rounds shift to grouping advice");
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(List.of()),
                Arguments.of(List.of("unknown-color")),
                Arguments.of(List.of(42, 13)),
                Arguments.of(List.of(" ", "\t"))
        );
    }

    private List<LightColor> copyCurrentSequence() {
        List<LightColor> current = puzzle.getCurrentSequence();
        assertFalse(current.isEmpty(), "Sequence should never be empty during active rounds");
        return new ArrayList<>(current);
    }

    private void solveCompletely() {
        int guard = 0;
        while (!puzzle.isSolved()) {
            assertTrue(puzzle.attempt(copyCurrentSequence()), "Expected round " + guard + " to succeed");
            guard++;
            assertTrue(guard <= 10, "Guard exceeded while solving puzzle");
        }
    }

    private void solveRound() {
        assertTrue(puzzle.attempt(copyCurrentSequence()));
    }
}
