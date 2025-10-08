package com.escapenexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Room {

    private final UUID id;
    private String name;
    private String description;
    private final List<Puzzle> puzzles = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private boolean locked;
    private Item keyRequired;
    private int hintLimit;

    public Room(String name, String description) {
        this(null, name, description);
    }

    public Room(UUID id, String name, String description) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.name = name;
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public List<Puzzle> getPuzzles() { return Collections.unmodifiableList(puzzles); }
    public List<Item> getItems() { return Collections.unmodifiableList(items); }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public Item getKeyRequired() { return keyRequired; }
    public void setKeyRequired(Item keyRequired) { this.keyRequired = keyRequired; }

    public int getHintLimit() { return hintLimit; }
    public void setHintLimit(int hintLimit) { this.hintLimit = hintLimit; }

    public void addPuzzle(Puzzle puzzle) {
        if (puzzle != null) puzzles.add(puzzle);
    }

    public void addItem(Item item) {
        if (item != null) items.add(item);
    }

    public boolean isCleared() {
        for (Puzzle p : puzzles) {
            if (!p.isSolved()) return false;
        }
        return true;
    }

    /** Existing title-based lookup (case-insensitive). */
    public Puzzle findPuzzle(String title) {
        if (title == null) return null;
        for (Puzzle p : puzzles) {
            if (title.equalsIgnoreCase(p.getTitle())) return p;
        }
        return null;
    }

    /** NEW: UUID-based lookup to support manager calls. */
    public Puzzle findPuzzleById(UUID puzzleId) {
        if (puzzleId == null) return null;
        for (Puzzle p : puzzles) {
            if (puzzleId.equals(p.getId())) return p;
        }
        return null;
    }

    public boolean unlock(Item key) {
        if (!locked) return false;
        if (keyRequired == null || key == null) return false;
        if (Objects.equals(keyRequired.getId(), key.getId())) {
            locked = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
