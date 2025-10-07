package com.escapenexus;

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
        return Optional.ofNullable(game)
                .map(Game::advanceRoom)
                .map(room -> room.getPuzzles().stream().findFirst().orElse(null))
                .orElse(null);
    }

    public void restartGame() {
        if (game != null) {
            game.start();
        }
    }

    public void resetProgress() {
        if (user != null) {
            user.clearProgress();
            user.getInventory().clear();
        }
    }

    public void bootstrapFromResources() {
        throw new UnsupportedOperationException("Persistence bootstrapping not implemented yet.");
    }
}
