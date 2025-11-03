package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: DataLoader JSON parsing for items, rooms, puzzles, key link resolution, and failure handling.
// - Happy path: minimal and typical resources load with correct room/puzzle counts and key references.
// - Negative cases: malformed resource throws an IOException with diagnostic context.
// - Boundary cases: puzzles without keys remain null; room key requirements link to item IDs.
// - Invariants: loaded puzzles retain UUID stability via name-based generation.
class PuzzleJsonLoadingTest {

    @Test
    @DisplayName("load_minimal_puzzleGraph_validLinks")
    void load_minimal_puzzleGraph_validLinks() throws IOException {
        DataLoader loader = new DataLoader();
        List<Game> games = loader.loadGamesFromResource("level_puzzle_minimal.json");

        assertEquals(1, games.size(), "Minimal resource should yield exactly one game");

        Game game = games.get(0);
        assertEquals(1, game.getRooms().size());
        assertEquals(1, game.getItems().size(), "All declared items should load into the game inventory");

        Room room = game.getRooms().get(0);
        assertFalse(room.isLocked(), "Room lock flag should respect JSON value");
        assertEquals(1, room.getPuzzles().size(), "Single puzzle expected for minimal layout");

        Puzzle puzzle = room.getPuzzles().get(0);
        assertNotNull(puzzle.getKeyProvided(), "Puzzle should provide the declared key");
        assertNotNull(room.getKeyRequired(), "Room should reference the same key for progression");
        assertEquals(room.getKeyRequired().getId(), puzzle.getKeyProvided().getId());
        assertEquals(1, puzzle.getHints().size());
        assertEquals("Watch every flash carefully.", puzzle.getHints().get(0));
    }

    @Test
    @DisplayName("load_typical_multiplePuzzles_allLinked")
    void load_typical_multiplePuzzles_allLinked() throws IOException {
        DataLoader loader = new DataLoader();
        List<Game> games = loader.loadGamesFromResource("level_puzzle_typical.json");

        assertFalse(games.isEmpty(), "Typical resource should load at least one game");
        Game game = games.get(0);

        assertEquals(2, game.getRooms().size(), "Typical map includes two rooms");
        assertEquals(2, game.getItems().size(), "Both declared keys should load");

        Room firstRoom = game.getRooms().get(0);
        Room secondRoom = game.getRooms().get(1);

        assertFalse(firstRoom.isLocked());
        assertTrue(secondRoom.isLocked());
        assertNotNull(secondRoom.getKeyRequired());

        List<Puzzle> firstRoomPuzzles = firstRoom.getPuzzles();
        assertEquals(2, firstRoomPuzzles.size(), "First room should contain two puzzles");

        Puzzle rebootPattern = firstRoomPuzzles.get(0);
        Puzzle riddle = firstRoomPuzzles.get(1);
        Puzzle alignment = secondRoom.getPuzzles().get(0);

        assertNotNull(rebootPattern.getKeyProvided(), "First puzzle grants the alpha key");
        assertEquals(secondRoom.getKeyRequired().getId(), rebootPattern.getKeyProvided().getId());

        assertNotNull(riddle.getKeyProvided(), "Second puzzle grants beta key");
        assertTrue(game.getItems().stream()
                .map(Item::getId)
                .anyMatch(id -> id.equals(riddle.getKeyProvided().getId())),
                "Beta key should be registered as a game item");

        assertNull(alignment.getKeyProvided(), "Final alignment puzzle should not provide a key");
        assertEquals(1, alignment.getHints().size());

        UUID expectedAlphaId = UUID.nameUUIDFromBytes("item-key-alpha".getBytes());
        assertEquals(expectedAlphaId, rebootPattern.getKeyProvided().getId(), "Key UUIDs should be name-based stable");
    }

    @Test
    @DisplayName("load_malformed_throwsSpecificException")
    void load_malformed_throwsSpecificException() {
        DataLoader loader = new DataLoader();
        IOException exception = assertThrows(IOException.class,
                () -> loader.loadGamesFromResource("level_puzzle_malformed.json"));
        assertTrue(exception.getMessage().contains("Failed to parse game data"));
    }
}
