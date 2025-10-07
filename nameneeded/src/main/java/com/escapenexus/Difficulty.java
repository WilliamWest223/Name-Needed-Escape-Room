package com.escapenexus;

public enum Difficulty {
    EASY(3),
    MEDIUM(2),
    HARD(1);

    private final int hintAllowance;

    Difficulty(int hintAllowance) {
        this.hintAllowance = hintAllowance;
    }

    public int getHintAllowance() {
        return hintAllowance;
    }
}
