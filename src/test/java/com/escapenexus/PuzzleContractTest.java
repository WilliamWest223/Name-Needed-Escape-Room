package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// Test Plan:
// - Coverage targets: shared Puzzle lifecycle (isSolved flag, attempt contract, key provisioning invariants).
// - Happy path: each concrete implementation transitions from unsolved to solved on its defined valid input.
// - Negative cases: null, empty, or incorrect inputs must not set the solved flag.
// - Boundary cases: ensure repeated valid attempts stay idempotent and do not throw after solving.
// - Invariants: puzzles start unsolved and cannot solve without their specific valid trigger.
class PuzzleContractTest {

    @ParameterizedTest(name = "{0} starts unsolved")
    @MethodSource("fixtures")
    void puzzle_startsUnsolved(PuzzleFixture fixture) {
        Puzzle puzzle = fixture.factory().get();
        assertFalse(puzzle.isSolved());
    }

    @ParameterizedTest(name = "{0} rejects invalid attempts")
    @MethodSource("fixtures")
    void puzzle_invalidAttempt_doesNotSolve(PuzzleFixture fixture) {
        Puzzle puzzle = fixture.factory().get();
        assertFalse(fixture.invalidAttempt().apply(puzzle));
        assertFalse(puzzle.isSolved());
    }

    @ParameterizedTest(name = "{0} solves exactly once")
    @MethodSource("fixtures")
    void puzzle_validAttempt_solvesOnce(PuzzleFixture fixture) {
        Puzzle puzzle = fixture.factory().get();
        assertTrue(fixture.validSolve().apply(puzzle), "First valid attempt should succeed");
        assertTrue(puzzle.isSolved(), "Puzzle must report solved after success");

        assertTrue(fixture.validSolve().apply(puzzle), "Subsequent valid attempt should be idempotent");
        assertTrue(puzzle.isSolved(), "Puzzle remains solved after repeated attempts");
    }

    private static Stream<PuzzleFixture> fixtures() {
        return Stream.of(
                lightPatternFixture(),
                riddleFixture(),
                mathFixture()
        );
    }

    private static PuzzleFixture lightPatternFixture() {
        Supplier<Puzzle> factory = () -> new LightPatternPuzzle(
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "Lights",
                "Repeat the pattern.",
                98765L
        );
        Function<Puzzle, Boolean> valid = puzzle -> solveLightPattern((LightPatternPuzzle) puzzle);
        Function<Puzzle, Boolean> invalid = puzzle -> ((LightPatternPuzzle) puzzle).attempt(List.of());
        return new PuzzleFixture("LightPatternPuzzle", factory, valid, invalid);
    }

    private static PuzzleFixture riddleFixture() {
        Supplier<Puzzle> factory = () -> new RiddlePuzzle(
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-ffffffffffff"),
                "Mag-Lock Riddle",
                "What has a bed but never sleeps?",
                List.of("river")
        );
        Function<Puzzle, Boolean> valid = puzzle -> puzzle.attempt("river");
        Function<Puzzle, Boolean> invalid = puzzle -> puzzle.attempt("mountain");
        return new PuzzleFixture("RiddlePuzzle", factory, valid, invalid);
    }

    private static PuzzleFixture mathFixture() {
        Supplier<Puzzle> factory = () -> new MathPuzzle(
                UUID.fromString("99999999-8888-7777-6666-555555555555"),
                "Calibration",
                "Add two numbers.",
                12.0,
                0.0
        );
        Function<Puzzle, Boolean> valid = puzzle -> puzzle.attempt("12");
        Function<Puzzle, Boolean> invalid = puzzle -> puzzle.attempt("11.9");
        return new PuzzleFixture("MathPuzzle", factory, valid, invalid);
    }

    private static boolean solveLightPattern(LightPatternPuzzle puzzle) {
        int guard = 0;
        while (!puzzle.isSolved()) {
            List<LightColor> guess = new ArrayList<>(puzzle.getCurrentSequence());
            if (guess.isEmpty()) {
                return false;
            }
            if (!puzzle.attempt(guess)) {
                return false;
            }
            guard++;
            if (guard > 10) {
                return false;
            }
        }
        return true;
    }

    private record PuzzleFixture(
            String name,
            Supplier<Puzzle> factory,
            Function<Puzzle, Boolean> validSolve,
            Function<Puzzle, Boolean> invalidAttempt
    ) {
        @Override
        public String toString() {
            return name;
        }
    }
}
