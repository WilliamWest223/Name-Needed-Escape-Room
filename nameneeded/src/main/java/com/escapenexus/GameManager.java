package com.escapenexus;

import java.time.Duration;
import java.util.*;

public class GameManager {
    private final Map<String, Game> games = new HashMap<>();

    public Game createGameFromRooms(List<Room> rooms, Duration timeLimit) {
        String gameId = UUID.randomUUID().toString();
        List<Room> safeRooms = (rooms == null) ? new ArrayList<Room>() : new ArrayList<Room>(rooms);
        Game game = new Game(gameId, safeRooms, timeLimit);
        games.put(gameId, game);
        return game;
    }

    public Optional<Game> getGame(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    public boolean attemptPuzzle(String gameId, String puzzleIdOrTitle, Object attempt) {
        Game game = games.get(gameId);
        if (game == null) return false;

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

        // If the room becomes cleared, try to advance
        if (solvedNow && room.isCleared()) {
            game.advanceIfSolved();
        }
        return solvedNow;
    }

    public boolean advanceIfSolved(String gameId) {
        Game game = games.get(gameId);
        return game != null && game.advanceIfSolved();
    }

    public void endGame(String gameId) {
        games.remove(gameId);
    }
}
