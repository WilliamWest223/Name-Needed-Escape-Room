package com.escapenexus;

public enum Difficulty {
    EASY(3),
    MEDIUM(2),
    HARD(1);

    private final int hintLimit;

    Difficulty(int hintLimit) {
        this.hintLimit = hintLimit;
    }

    public int getHintLimit() {
        return hintLimit;
    }
}
