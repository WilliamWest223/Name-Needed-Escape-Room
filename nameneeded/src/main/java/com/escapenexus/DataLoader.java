package com.escapenexus;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DataLoader {

    private static final String DEFAULT_RESOURCE = "com/escapenexus/game-data.json";
    private final JSONParser parser = new JSONParser();

    public List<Game> loadGames(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parseGames(reader);
        }
    }

    public List<Game> loadGamesFromResource() throws IOException {
        return loadGamesFromResource(DEFAULT_RESOURCE);
    }

    public List<Game> loadGamesFromResource(String resourcePath) throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        ClassLoader classLoader = DataLoader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                return parseGames(reader);
            }
        }
    }

    private List<Game> parseGames(Reader reader) throws IOException {
        try {
            Object parsed = parser.parse(reader);
            if (!(parsed instanceof JSONObject jsonObject)) {
                return List.of();
            }
            JSONArray gamesArray = getArray(jsonObject, "games");
            if (gamesArray == null || gamesArray.isEmpty()) {
                return List.of();
            }

            List<Game> games = new ArrayList<>();
            for (Object element : gamesArray) {
                if (!(element instanceof JSONObject gameJson)) {
                    continue;
                }
                Game game = toGame(gameJson);
                if (game != null) {
                    games.add(game);
                }
            }
            return games;
        } catch (ParseException ex) {
            throw new IOException("Failed to parse game data", ex);
        }
    }

    private Game toGame(JSONObject gameJson) {
        String idRef = getString(gameJson, "id");
        String title = getString(gameJson, "title");
        if (title == null || title.isBlank()) {
            return null;
        }
        String description = getString(gameJson, "description");
        Difficulty difficulty = parseDifficulty(getString(gameJson, "difficulty"));
        int timeLimitMinutes = getInt(gameJson.get("timeLimitMinutes"), 30);
        int maxPlayers = getInt(gameJson.get("maxPlayers"), 1);

        Game game = new Game(toUuid(idRef, title), title, description, difficulty, timeLimitMinutes, maxPlayers);
        game.setDifficulty(difficulty);
        game.setTimeLimit(Duration.ofMinutes(timeLimitMinutes));

        Map<String, Item> itemsById = loadItems(game, getArray(gameJson, "items"));
        loadRooms(game, getArray(gameJson, "rooms"), itemsById);
        return game;
    }

    private Map<String, Item> loadItems(Game game, JSONArray itemsArray) {
        Map<String, Item> items = new HashMap<>();
        if (itemsArray == null) {
            return items;
        }
        for (Object element : itemsArray) {
            if (!(element instanceof JSONObject itemJson)) {
                continue;
            }
            Item item = toItem(itemJson);
            if (item != null) {
                items.put(getString(itemJson, "id"), item);
                game.addItem(item);
            }
        }
        return items;
    }

    private void loadRooms(Game game, JSONArray roomsArray, Map<String, Item> itemsById) {
        if (roomsArray == null) {
            return;
        }
        for (Object element : roomsArray) {
            if (!(element instanceof JSONObject roomJson)) {
                continue;
            }
            Room room = toRoom(roomJson, itemsById);
            if (room != null) {
                game.addRoom(room);
            }
        }
    }

    private Item toItem(JSONObject itemJson) {
        String idRef = getString(itemJson, "id");
        String name = getString(itemJson, "name");
        if (name == null || name.isBlank()) {
            return null;
        }
        String description = getString(itemJson, "description");
        boolean portable = getBoolean(itemJson.get("portable"), true);
        boolean key = getBoolean(itemJson.get("key"), false);
        ItemState state = parseItemState(getString(itemJson, "state"));
        return new Item(toUuid(idRef, name), name, description, portable, key, state);
    }

    private Room toRoom(JSONObject roomJson, Map<String, Item> itemsById) {
        String idRef = getString(roomJson, "id");
        String name = getString(roomJson, "name");
        if (name == null || name.isBlank()) {
            return null;
        }
        String description = getString(roomJson, "description");
        Room room = new Room(toUuid(idRef, name), name, description);

        Boolean locked = getBooleanObject(roomJson.get("locked"));
        if (locked != null) {
            room.setLocked(locked);
        }

        int hintLimit = getInt(roomJson.get("hintLimit"), -1);
        if (hintLimit >= 0) {
            room.setHintLimit(hintLimit);
        }

        String keyRequiredRef = getString(roomJson, "keyRequired");
        if (keyRequiredRef != null) {
            Item keyRequired = itemsById.get(keyRequiredRef);
            if (keyRequired != null) {
                room.setKeyRequired(keyRequired);
            }
        }

        JSONArray roomItems = getArray(roomJson, "items");
        if (roomItems != null) {
            for (Object itemRef : roomItems) {
                String ref = Objects.toString(itemRef, null);
                Item item = itemsById.get(ref);
                if (item != null) {
                    room.addItem(item);
                }
            }
        }

        JSONArray puzzlesArray = getArray(roomJson, "puzzles");
        if (puzzlesArray != null) {
            for (Object element : puzzlesArray) {
                if (!(element instanceof JSONObject puzzleJson)) {
                    continue;
                }
                Puzzle puzzle = toPuzzle(puzzleJson, itemsById);
                if (puzzle != null) {
                    room.addPuzzle(puzzle);
                }
            }
        }
        return room;
    }

    private Puzzle toPuzzle(JSONObject puzzleJson, Map<String, Item> itemsById) {
        String idRef = getString(puzzleJson, "id");
        String title = getString(puzzleJson, "title");
        if (title == null || title.isBlank()) {
            return null;
        }
        String description = getString(puzzleJson, "description");
        Puzzle puzzle = new Puzzle(toUuid(idRef, title), title, description);

        JSONArray hints = getArray(puzzleJson, "hints");
        if (hints != null) {
            for (Object hint : hints) {
                if (hint != null) {
                    puzzle.addHint(hint.toString());
                }
            }
        }

        String keyProvidedRef = getString(puzzleJson, "keyProvided");
        if (keyProvidedRef != null) {
            Item key = itemsById.get(keyProvidedRef);
            if (key != null) {
                puzzle.setKeyProvided(key);
            }
        }
        return puzzle;
    }

    private UUID toUuid(String idRef, String fallback) {
        String seed = idRef != null && !idRef.isBlank() ? idRef : fallback;
        if (seed == null || seed.isBlank()) {
            return UUID.randomUUID();
        }
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return Difficulty.MEDIUM;
        }
        try {
            return Difficulty.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Difficulty.MEDIUM;
        }
    }

    private ItemState parseItemState(String value) {
        if (value == null || value.isBlank()) {
            return ItemState.NEW;
        }
        try {
            return ItemState.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ItemState.NEW;
        }
    }

    private JSONArray getArray(JSONObject object, String key) {
        Object value = object.get(key);
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        return null;
    }

    private String getString(JSONObject object, String key) {
        Object value = object.get(key);
        return value != null ? value.toString() : null;
    }

    private int getInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBoolean(Object value, boolean defaultValue) {
        Boolean booleanObject = getBooleanObject(value);
        return booleanObject != null ? booleanObject : defaultValue;
    }

    private Boolean getBooleanObject(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }
}
