package com.escapenexus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String username;
    private String email;
    private final Inventory inventory = new Inventory();
    private Room currentRoom;
    private final Map<UUID, Progress> progressByRoomId = new HashMap<>();
    private Difficulty difficulty = Difficulty.MEDIUM;

    public User(String username) {
        this(username, "");
    }

    public User(String username, String email) {
        this(null, username, email);
    }

    public User(UUID id, String username, String email) {
        this.id = id != null ? id : UUID.randomUUID();
        this.username = username;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty != null) {
            this.difficulty = difficulty;
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public Map<UUID, Progress> getProgressByRoomId() {
        return Collections.unmodifiableMap(progressByRoomId);
    }

    public Progress getOrCreateProgress(UUID roomId) {
        return progressByRoomId.computeIfAbsent(roomId, id -> new Progress());
    }

    public Progress getProgress(UUID roomId) {
        return progressByRoomId.get(roomId);
    }

    public void moveTo(Room room) {
        if (room == null) {
            return;
        }
        this.currentRoom = room;
        progressByRoomId.computeIfAbsent(room.getId(), id -> new Progress());
    }

    public boolean pickUp(Item item) {
        if (item == null || !item.isPortable()) {
            return false;
        }
        return inventory.add(item);
    }

    public boolean useItem(Item item, Object target) {
        if (item == null) {
            return false;
        }
        if (!inventory.contains(item)) {
            return false;
        }
        return item.use(this, target);
    }

    public String requestHint(Room room) {
        if (room == null || room.getPuzzles().isEmpty()) {
            return "No hints available.";
        }

        Progress progress = getOrCreateProgress(room.getId());
        Puzzle puzzle = room.getPuzzles().get(0);
        UUID puzzleId = puzzle.getId();

        if (progress.getCurrentPuzzleId() == null) {
            progress.setCurrentPuzzleId(puzzleId);
        }

        int usedHints = progress.getHintsUsed(puzzleId);
        int hintLimit = Math.max(0, room.getHintLimit());
        int remaining = hintLimit - usedHints;
        if (remaining <= 0) {
            return "No hints left.";
        }

        progress.setHintCount(puzzleId, usedHints + 1);
        return puzzle.giveHint();
    }

    public void clearProgress() {
        progressByRoomId.clear();
    }

    public void putProgress(UUID roomId, Progress progress) {
        if (roomId != null && progress != null) {
            progressByRoomId.put(roomId, progress);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
