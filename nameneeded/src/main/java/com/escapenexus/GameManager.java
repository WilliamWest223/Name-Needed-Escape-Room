package com.escapenexus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

/**
 * GameManager acts as a simple facade for common game lifecycle operations:
 * start a new game, load a saved game, and save the current game.
 */
public class GameManager {
    private final Map<String, Game> games = new HashMap<>();

    // Facade state and collaborators
    private final DataLoader loader = new DataLoader();
    private final DataWriter writer = new DataWriter();
    private Game currentGame;

    // Default save location (relative to project root)
    private static final Path DEFAULT_SAVE_PATH = Paths.get("saves", "current-game.json");

    public Game createGameFromRooms(List<Room> rooms, Duration timeLimit) {
        String gameId = UUID.randomUUID().toString();
        List<Room> safeRooms = (rooms == null) ? new ArrayList<Room>() : new ArrayList<Room>(rooms);
        Game game = new Game(gameId, safeRooms, timeLimit);
        games.put(gameId, game);
        this.currentGame = game;
        return game;
    }

    public Optional<Game> getGame(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    public Optional<Game> getCurrentGame() {
        return Optional.ofNullable(currentGame);
    }

    public boolean attemptPuzzle(String gameId, String puzzleIdOrTitle, Object attempt) {
        Game game = games.get(gameId);
        if (game == null) return false;

        // NOTE: Game#getCurrentRoom is not implemented in this codebase yet.
        // This method remains for future use; facade methods below do not depend on it.
        try {
            Room room = game.getCurrentRoom();
            if (room == null) return false;

            Puzzle puzzle = null;
            if (puzzleIdOrTitle != null) {
                try {
                    UUID pid = UUID.fromString(puzzleIdOrTitle);
                    puzzle = room.findPuzzleById(pid);
                } catch (IllegalArgumentException ignored) {
                    puzzle = room.findPuzzle(puzzleIdOrTitle);
                }
            }

            if (puzzle == null) return false;
            boolean solvedNow = puzzle.attempt(attempt);

            if (solvedNow && room.isCleared()) {
                game.advanceIfSolved();
            }
            return solvedNow;
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    public boolean advanceIfSolved(String gameId) {
        Game game = games.get(gameId);
        try {
            return game != null && game.advanceIfSolved();
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    public void endGame(String gameId) {
        games.remove(gameId);
        if (currentGame != null && gameId != null && currentGame.getId() != null
                && gameId.equals(currentGame.getId().toString())) {
            currentGame = null;
        }
    }

    // ===== Facade methods =====

    /** Start a new default game and store as current. */
    public Game startNewGame() {
        return startNewGame(Difficulty.MEDIUM);
    }

    /** Start a new game with the given difficulty. */
    public Game startNewGame(Difficulty difficulty) {
        Difficulty d = (difficulty != null) ? difficulty : Difficulty.MEDIUM;
        Game game = GameFactory.createDefaultThreeRoomGame(d);
        games.put(game.getId().toString(), game);
        this.currentGame = game;
        return game;
    }

    /** Load a game from the default save file, or fallback to resources/default. */
    public Game loadGame() {
        // Try default save file first
        if (Files.exists(DEFAULT_SAVE_PATH)) {
            try {
                List<Game> loaded = loader.loadGames(DEFAULT_SAVE_PATH);
                if (!loaded.isEmpty()) {
                    Game game = loaded.get(0);
                    games.put(game.getId().toString(), game);
                    this.currentGame = game;
                    return game;
                }
            } catch (IOException e) {
                // fall through to resource/default
            }
        }

        // Fallback: bootstrap from resource or factory
        try {
            List<Game> fromResource = loader.loadGamesFromResource();
            if (!fromResource.isEmpty()) {
                Game game = fromResource.get(0);
                games.put(game.getId().toString(), game);
                this.currentGame = game;
                return game;
            }
        } catch (IOException ignored) {
        }

        Game game = GameFactory.createDefaultThreeRoomGame(Difficulty.MEDIUM);
        games.put(game.getId().toString(), game);
        this.currentGame = game;
        return game;
    }

    /** Load a game from the specified file path. */
    public Game loadGame(Path file) {
        Objects.requireNonNull(file, "file");
        try {
            List<Game> loaded = loader.loadGames(file);
            if (loaded.isEmpty()) {
                throw new IllegalStateException("No games found in save file: " + file);
            }
            Game game = loaded.get(0);
            games.put(game.getId().toString(), game);
            this.currentGame = game;
            return game;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load game from: " + file, e);
        }
    }

    /** Save the current game to the default save file. */
    public void saveCurrentGame() {
        if (currentGame == null) {
            throw new IllegalStateException("No current game to save");
        }
        try {
            writer.writeGame(DEFAULT_SAVE_PATH, currentGame);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save game", e);
        }
    }

    /** Save the current game to the specified file. */
    public void saveCurrentGame(Path file) {
        if (currentGame == null) {
            throw new IllegalStateException("No current game to save");
        }
        Objects.requireNonNull(file, "file");
        try {
            writer.writeGame(file, currentGame);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save game to: " + file, e);
        }
    }

    /** Save the provided game to the default save file. */
    public void saveGame(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("game");
        }
        try {
            writer.writeGame(DEFAULT_SAVE_PATH, game);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save game", e);
        }
    }

    /**
     * Display basic instructions (string returned for the caller to present).
     */
    public Object showInstructions() {
        return "Solve puzzles in each room. Use hints wisely based on difficulty. Keys from puzzles unlock subsequent rooms.";
    }

    /** Exit the current game session (in-memory cleanup). */
    public void exitGame() {
        this.currentGame = null;
    }
}
