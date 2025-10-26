package com.escapenexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** 5-light, 5-round Simon-like memory puzzle. */
public class LightPatternPuzzle extends Puzzle {
    private static final int TOTAL_ROUNDS = 5;
    private final Random rng;
    private final List<LightColor> sequence = new ArrayList<>();
    private int currentRound = 0;

    public LightPatternPuzzle(UUID id, String title, String description, long seed) {
        super(id, title, description);
        this.rng = new Random(seed);
        appendRandom();
    }

    private void appendRandom() {
        sequence.add(LightColor.fromIndex(rng.nextInt(LightColor.values().length)));
    }

    /** Sequence to reproduce for the current round. */
    public List<LightColor> getCurrentSequence() {
        return Collections.unmodifiableList(sequence.subList(0, currentRound + 1));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean attempt(Object input) {
        if (isSolved()) {
            return true;
        }

        List<LightColor> guess = null;
        if (input instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof LightColor) {
                guess = (List<LightColor>) list;
            } else if (first instanceof String) {
                List<LightColor> parsed = new ArrayList<>();
                for (Object element : list) {
                    try {
                        parsed.add(LightColor.fromString(String.valueOf(element)));
                    } catch (Exception exception) {
                        return false;
                    }
                }
                guess = parsed;
            }
        }
        if (guess == null) {
            return false;
        }

        List<LightColor> target = getCurrentSequence();
        if (guess.size() != target.size()) {
            return false;
        }
        for (int index = 0; index < target.size(); index++) {
            if (guess.get(index) != target.get(index)) {
                return false;
            }
        }

        if (currentRound + 1 >= TOTAL_ROUNDS) {
            setSolved(true);
            return true;
        }

        currentRound++;
        appendRandom();
        return true;
    }

    @Override
    public String giveHint() {
        if (!getHints().isEmpty()) {
            return super.giveHint();
        }
        return switch (currentRound) {
            case 0, 1 -> "Count the flashes and replay in order.";
            case 2, 3 -> "Chunk colors into small groups.";
            default -> "Say each color out loud as you input it.";
        };
    }
}
