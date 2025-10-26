package com.escapenexus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class GameResults {

    private final UUID id;
    private UUID userId;
    private Duration duration;
    private boolean success;
    private LocalDateTime completedAt;

    public GameResults(UUID userId, Duration duration, boolean success, LocalDateTime completedAt) {
        this(UUID.randomUUID(), userId, duration, success, completedAt);
    }

    public GameResults(UUID id, UUID userId, Duration duration, boolean success, LocalDateTime completedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId;
        this.duration = duration;
        this.success = success;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Duration getDuration() {
        return duration;
    }

    public boolean isSuccess() {
        return success;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GameResults that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
