package com.escapenexus;

import java.util.UUID;

public class MathPuzzle extends Puzzle {
    private final double answer;
    private final double tolerance;

    public MathPuzzle(UUID id, String title, String prompt, double answer, double tolerance) {
        super(id, title, prompt);
        this.answer = answer;
        this.tolerance = Math.max(0.0, tolerance);
    }

    @Override
    public boolean attempt(Object input) {
        if (isSolved()) {
            return true;
        }
        if (input == null) {
            return false;
        }
        try {
            double value = (input instanceof Number)
                    ? ((Number) input).doubleValue()
                    : Double.parseDouble(String.valueOf(input).trim());
            boolean success = Math.abs(value - answer) <= tolerance;
            if (success) {
                setSolved(true);
            }
            return success;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public String giveHint() {
        if (!getHints().isEmpty()) {
            return super.giveHint();
        }
        return "Combine the displayed values exactly as the prompt describes.";
    }
}
