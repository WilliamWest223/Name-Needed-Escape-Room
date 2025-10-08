package com.escapenexus;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class EscapeGame {

    private static EscapeGame instance;
    private User user;
    private Game game;

    private EscapeGame() {
    }

    public static synchronized EscapeGame getInstance() {
        if (instance == null) {
            instance = new EscapeGame();
        }
        return instance;
    }

    public void login(User user) {
        this.user = user;
    }

    public void logout() {
        this.user = null;
    }

    public User getUser() {
        return user;
    }

    public Game getGame() {
        return game;
    }

    public Puzzle getNextPuzzle() {
        return Optional.ofNullable(user)
                .map(User::getCurrentRoom)
                .filter(room -> !room.getPuzzles().isEmpty())
                .map(room -> room.getPuzzles().get(0))
                .orElse(null);
    }

    public void restartGame() {
        if (game != null) {
            game.start();
        }
    }

    public void newSinglePlayerSession(String username, Difficulty difficulty) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        Difficulty resolvedDifficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        this.user = new User(username);
        this.user.setDifficulty(resolvedDifficulty);

        this.game = GameFactory.createDefaultThreeRoomGame(resolvedDifficulty);
        game.start();

        for (Room room : game.getRooms()) {
            room.setHintLimit(resolvedDifficulty.getHintAllowance());
        }

        List<Room> rooms = game.getRooms();
        if (!rooms.isEmpty()) {
            user.moveTo(rooms.get(0));
        }
    }

    public boolean attemptCurrentPuzzle(Object input) {
        if (user == null || game == null) {
            return false;
        }

        Room currentRoom = user.getCurrentRoom();
        if (currentRoom == null || currentRoom.getPuzzles().isEmpty()) {
            return false;
        }

        Puzzle puzzle = currentRoom.getPuzzles().get(0);
        boolean success = puzzle.attempt(input);
        if (!success) {
            return false;
        }

        Progress progress = user.getOrCreateProgress(currentRoom.getId());
        progress.setPuzzleSolved(puzzle.getId(), true);

        Item providedKey = puzzle.getKeyProvided();
        if (providedKey != null) {
            user.pickUp(providedKey);
            Room nextRoom = findNextRoom(currentRoom);
            if (nextRoom != null) {
                nextRoom.unlock(providedKey);
            }
        }

        if (currentRoom.isCleared()) {
            Room nextRoom = game.advanceRoom();
            if (nextRoom != null && nextRoom != currentRoom) {
                user.moveTo(nextRoom);
            }
        }

        return true;
    }

    public void resetProgress() {
        if (user != null) {
            user.clearProgress();
            user.getInventory().clear();
        }
    }

    public void bootstrapFromResources() {
        DataLoader loader = new DataLoader();
        try {
            List<Game> games = loader.loadGamesFromResource();
            if (!games.isEmpty()) {
                this.game = games.get(0);
            } else {
                this.game = GameFactory.createDefaultThreeRoomGame(Difficulty.MEDIUM);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to bootstrap game data", exception);
        }
    }

    private Room findNextRoom(Room currentRoom) {
        if (game == null || currentRoom == null) {
            return null;
        }
        List<Room> rooms = game.getRooms();
        int index = rooms.indexOf(currentRoom);
        if (index >= 0 && index + 1 < rooms.size()) {
            return rooms.get(index + 1);
        }
        return null;
    }
}
