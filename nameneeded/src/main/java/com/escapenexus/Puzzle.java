package com.escapenexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Puzzle {

    private final UUID id;
    private String title;
    private String description;
    private final List<String> hints = new ArrayList<>();
    private boolean solved;
    private Item keyProvided;

    public Puzzle(String title, String description) {
        this(null, title, description);
    }

    public Puzzle(UUID id, String title, String description) {
        this.id = id != null ? id : UUID.randomUUID();
        this.title = title;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getHints() {
        return Collections.unmodifiableList(hints);
    }

    public void addHint(String hint) {
        if (hint != null) {
            hints.add(hint);
        }
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public Item getKeyProvided() {
        return keyProvided;
    }

    public void setKeyProvided(Item keyProvided) {
        this.keyProvided = keyProvided;
    }

    public boolean attempt(Object input) {
        if (input == null) {
            return false;
        }
        solved = true;
        return true;
    }

    private int __hintIndex = 0;

    public String giveHint() {
        List<String> resolvedHints = getHints();
        if (resolvedHints == null || resolvedHints.isEmpty()) {
            return "No hints available.";
        }
        if (__hintIndex >= resolvedHints.size()) {
            return "No hints available.";
        }
        return resolvedHints.get(__hintIndex++);
    }

    public void reset() {
        this.solved = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Puzzle puzzle)) {
            return false;
        }
        return Objects.equals(id, puzzle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
