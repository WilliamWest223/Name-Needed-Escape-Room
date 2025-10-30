package com.escapenexus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class RiddlePuzzle extends Puzzle {
    private final Set<String> acceptable = new HashSet<>();

    public RiddlePuzzle(UUID id, String title, String prompt, Collection<String> acceptableAnswers) {
        super(id, title, prompt);
        for (String answer : acceptableAnswers) {
            acceptable.add(normalize(answer));
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean attempt(Object input) {
        if (isSolved()) {
            return true;
        }
        if (input == null) {
            return false;
        }
        boolean success = acceptable.contains(normalize(String.valueOf(input)));
        if (success) {
            setSolved(true);
        }
        return success;
    }

    @Override
    public String giveHint() {
        if (!getHints().isEmpty()) {
            return super.giveHint();
        }
        return "Picture something with a bed, a mouth, and a foot that never leaves its place.";
    }
}
