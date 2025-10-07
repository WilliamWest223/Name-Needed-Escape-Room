package com.escapenexus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Game {

    private final UUID id;
    private String title;
    private String description;
    private final List<String> story = new ArrayList<>();
    private Difficulty difficulty;
    private Duration timeLimit;
    private final List<Room> rooms = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private GameStatus status = GameStatus.NOT_STARTED;
    private final List<GameResults> leaderboard = new ArrayList<>();

    public Game(String title, String description, Difficulty difficulty, int timeLimitMinutes, int maxPlayers) {
        this(null, title, description, difficulty, timeLimitMinutes, maxPlayers);
    }

    public Game(UUID id, String title, String description, Difficulty difficulty, int timeLimitMinutes, int maxPlayers) {
        this.id = id != null ? id : UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.timeLimit = Duration.ofMinutes(timeLimitMinutes);
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

    public List<String> getStory() {
        return Collections.unmodifiableList(story);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Duration getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Duration timeLimit) {
        this.timeLimit = timeLimit;
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public void addRoom(Room room) {
        if (room != null) {
            rooms.add(room);
        }
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(Item item) {
        if (item != null) {
            items.add(item);
        }
    }

    public GameStatus getStatus() {
        return status;
    }

    public List<GameResults> getLeaderboard() {
        return Collections.unmodifiableList(leaderboard);
    }

    public void addResult(GameResults result) {
        if (result != null) {
            leaderboard.add(result);
        }
    }

    public void start() {
        status = GameStatus.IN_PROGRESS;
    }

    public void end() {
        status = GameStatus.COMPLETED;
    }

    public Room getRoom(UUID roomId) {
        if (roomId == null) {
            return null;
        }
        return rooms.stream()
                .filter(room -> roomId.equals(room.getId()))
                .findFirst()
                .orElse(null);
    }

    public Item getItem(UUID itemId) {
        if (itemId == null) {
            return null;
        }
        return items.stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElseGet(() -> rooms.stream()
                        .flatMap(room -> room.getItems().stream())
                        .filter(item -> itemId.equals(item.getId()))
                        .findFirst()
                        .orElse(null));
    }

    public Room advanceRoom() {
        return rooms.stream()
                .filter(room -> !room.isCleared())
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Game game)) {
            return false;
        }
        return Objects.equals(id, game.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
