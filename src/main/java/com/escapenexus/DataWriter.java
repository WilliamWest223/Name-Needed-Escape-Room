package com.escapenexus;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DataWriter {

    public void writeGames(Path file, List<Game> games) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(games, "games");

        JSONObject root = toRootObject(games);
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(root.toJSONString());
        }
    }

    public void writeGame(Path file, Game game) throws IOException {
        Objects.requireNonNull(game, "game");
        writeGames(file, List.of(game));
    }

    public String toJson(List<Game> games) {
        Objects.requireNonNull(games, "games");
        return toRootObject(games).toJSONString();
    }

    private JSONObject toRootObject(List<Game> games) {
        JSONArray gamesArray = new JSONArray();
        for (Game game : games) {
            if (game != null) {
                gamesArray.add(toGameObject(game));
            }
        }
        JSONObject root = new JSONObject();
        root.put("games", gamesArray);
        return root;
    }

    private JSONObject toGameObject(Game game) {
        JSONObject gameJson = new JSONObject();
        gameJson.put("id", uuidToString(game.getId()));
        gameJson.put("title", game.getTitle());
        gameJson.put("description", game.getDescription());

        Difficulty difficulty = game.getDifficulty() != null ? game.getDifficulty() : Difficulty.MEDIUM;
        gameJson.put("difficulty", difficulty.name());

        Duration timeLimit = game.getTimeLimit();
        long minutes = timeLimit != null ? timeLimit.toMinutes() : 0;
        gameJson.put("timeLimitMinutes", minutes);
        gameJson.put("maxPlayers", 1);

        Map<UUID, Item> itemsById = collectAllItems(game);
        gameJson.put("items", toItemsArray(itemsById));
        gameJson.put("rooms", toRoomsArray(game.getRooms(), itemsById));
        return gameJson;
    }

    private Map<UUID, Item> collectAllItems(Game game) {
        Map<UUID, Item> items = new LinkedHashMap<>();
        for (Item item : game.getItems()) {
            putItem(items, item);
        }
        for (Room room : game.getRooms()) {
            if (room == null) {
                continue;
            }
            putItem(items, room.getKeyRequired());
            for (Item item : room.getItems()) {
                putItem(items, item);
            }
            for (Puzzle puzzle : room.getPuzzles()) {
                if (puzzle != null) {
                    putItem(items, puzzle.getKeyProvided());
                }
            }
        }
        return items;
    }

    private void putItem(Map<UUID, Item> items, Item item) {
        if (item == null || item.getId() == null) {
            return;
        }
        items.putIfAbsent(item.getId(), item);
    }

    private JSONArray toItemsArray(Map<UUID, Item> itemsById) {
        JSONArray itemsArray = new JSONArray();
        for (Item item : itemsById.values()) {
            JSONObject itemJson = new JSONObject();
            itemJson.put("id", uuidToString(item.getId()));
            itemJson.put("name", item.getName());
            itemJson.put("description", item.getDescription());
            itemJson.put("portable", item.isPortable());
            itemJson.put("key", item.isKey());
            ItemState state = item.getState() != null ? item.getState() : ItemState.NEW;
            itemJson.put("state", state.name());
            itemsArray.add(itemJson);
        }
        return itemsArray;
    }

    private JSONArray toRoomsArray(List<Room> rooms, Map<UUID, Item> itemsById) {
        JSONArray roomsArray = new JSONArray();
        for (Room room : rooms) {
            if (room != null) {
                roomsArray.add(toRoomObject(room, itemsById));
            }
        }
        return roomsArray;
    }

    private JSONObject toRoomObject(Room room, Map<UUID, Item> itemsById) {
        JSONObject roomJson = new JSONObject();
        roomJson.put("id", uuidToString(room.getId()));
        roomJson.put("name", room.getName());
        roomJson.put("description", room.getDescription());
        roomJson.put("locked", room.isLocked());
        roomJson.put("hintLimit", room.getHintLimit());

        Item keyRequired = room.getKeyRequired();
        if (keyRequired != null) {
            roomJson.put("keyRequired", uuidToString(keyRequired.getId()));
            putItem(itemsById, keyRequired);
        }

        JSONArray items = new JSONArray();
        for (Item item : room.getItems()) {
            if (item != null) {
                items.add(uuidToString(item.getId()));
                putItem(itemsById, item);
            }
        }
        roomJson.put("items", items);

        JSONArray puzzles = new JSONArray();
        for (Puzzle puzzle : room.getPuzzles()) {
            if (puzzle != null) {
                puzzles.add(toPuzzleObject(puzzle, itemsById));
            }
        }
        roomJson.put("puzzles", puzzles);
        return roomJson;
    }

    private JSONObject toPuzzleObject(Puzzle puzzle, Map<UUID, Item> itemsById) {
        JSONObject puzzleJson = new JSONObject();
        puzzleJson.put("id", uuidToString(puzzle.getId()));
        puzzleJson.put("title", puzzle.getTitle());
        puzzleJson.put("description", puzzle.getDescription());

        JSONArray hints = new JSONArray();
        for (String hint : puzzle.getHints()) {
            hints.add(hint);
        }
        puzzleJson.put("hints", hints);

        Item keyProvided = puzzle.getKeyProvided();
        if (keyProvided != null) {
            puzzleJson.put("keyProvided", uuidToString(keyProvided.getId()));
            putItem(itemsById, keyProvided);
        }
        return puzzleJson;
    }

    private String uuidToString(UUID id) {
        return id != null ? id.toString() : null;
    }
}
