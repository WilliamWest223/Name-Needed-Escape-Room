package com.escapenexus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Progress {

    private final Map<UUID, Boolean> puzzlesSolved = new HashMap<>();
    private final Map<UUID, Integer> puzzleHints = new HashMap<>();
    private Duration duration = Duration.ZERO;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UUID currentPuzzleId;

    public Map<UUID, Boolean> getPuzzlesSolved() {
        return Collections.unmodifiableMap(puzzlesSolved);
    }

    public Map<UUID, Integer> getPuzzleHints() {
        return Collections.unmodifiableMap(puzzleHints);
    }

    public void setPuzzleSolved(UUID puzzleId, boolean solved) {
        if (puzzleId != null) {
            puzzlesSolved.put(puzzleId, solved);
        }
    }

    public void setHintCount(UUID puzzleId, int hintsUsed) {
        if (puzzleId != null) {
            puzzleHints.put(puzzleId, Math.max(0, hintsUsed));
        }
    }

    public boolean isPuzzleSolved(UUID puzzleId) {
        return Boolean.TRUE.equals(puzzlesSolved.get(puzzleId));
    }

    public int getHintsUsed(UUID puzzleId) {
        return puzzleHints.getOrDefault(puzzleId, 0);
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration != null ? duration : Duration.ZERO;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public UUID getCurrentPuzzleId() {
        return currentPuzzleId;
    }

    public void setCurrentPuzzleId(UUID currentPuzzleId) {
        this.currentPuzzleId = currentPuzzleId;
    }

    public int getHintCountForRoom(Room room) {
        if (room == null) {
            return 0;
        }
        return room.getPuzzles().stream()
                .map(Puzzle::getId)
                .mapToInt(this::getHintsUsed)
                .sum();
    }
}
