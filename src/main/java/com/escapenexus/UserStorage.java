package com.escapenexus;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Persists users and their progress to a simple JSON file.
 * File: saves/users.json
 */
public final class UserStorage {

    private static final Path USERS_FILE = Paths.get("saves", "users.json");

    private final JSONParser parser = new JSONParser();

    public List<User> loadUsers() {
        if (!Files.exists(USERS_FILE)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(USERS_FILE, StandardCharsets.UTF_8)) {
            Object parsed = parser.parse(reader);
            if (!(parsed instanceof JSONObject root)) {
                return new ArrayList<>();
            }

            JSONArray usersArray = (JSONArray) root.get("users");
            if (usersArray == null) {
                return new ArrayList<>();
            }

            List<User> users = new ArrayList<>();
            for (Object element : usersArray) {
                if (!(element instanceof JSONObject u)) continue;
                User user = fromJsonUser(u);
                if (user != null) users.add(user);
            }
            return users;
        } catch (IOException | ParseException e) {
            return new ArrayList<>();
        }
    }

    public void saveUsers(List<User> users) {
        Objects.requireNonNull(users, "users");
        JSONObject root = new JSONObject();
        JSONArray usersArray = new JSONArray();
        for (User user : users) {
            if (user != null) {
                usersArray.add(toJsonUser(user));
            }
        }
        root.put("users", usersArray);

        try {
            Path parent = USERS_FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (Writer writer = Files.newBufferedWriter(USERS_FILE, StandardCharsets.UTF_8)) {
                writer.write(root.toJSONString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save users", e);
        }
    }

    private User fromJsonUser(JSONObject u) {
        String idStr = Objects.toString(u.get("id"), null);
        UUID id = idStr != null ? parseUuid(idStr) : null;
        String username = Objects.toString(u.get("username"), null);
        String email = Objects.toString(u.get("email"), "");
        String diffStr = Objects.toString(u.get("difficulty"), null);

        User user = new User(id, username, email);
        if (diffStr != null) {
            try {
                user.setDifficulty(Difficulty.valueOf(diffStr));
            } catch (Exception ignored) { }
        }

        // Progress by room
        JSONObject progressRoot = (JSONObject) u.get("progressByRoomId");
        if (progressRoot != null) {
            for (Object key : progressRoot.keySet()) {
                UUID roomId = parseUuid(Objects.toString(key, null));
                if (roomId == null) continue;
                JSONObject pObj = (JSONObject) progressRoot.get(key);
                Progress p = new Progress();

                // solved
                JSONObject solvedMap = (JSONObject) pObj.get("puzzlesSolved");
                if (solvedMap != null) {
                    for (Object pidKey : solvedMap.keySet()) {
                        UUID puzzleId = parseUuid(Objects.toString(pidKey, null));
                        boolean solved = Boolean.TRUE.equals(solvedMap.get(pidKey)) ||
                                (solvedMap.get(pidKey) instanceof String s && Boolean.parseBoolean(s));
                        if (puzzleId != null) p.setPuzzleSolved(puzzleId, solved);
                    }
                }

                // hints
                JSONObject hintsMap = (JSONObject) pObj.get("puzzleHints");
                if (hintsMap != null) {
                    for (Object pidKey : hintsMap.keySet()) {
                        UUID puzzleId = parseUuid(Objects.toString(pidKey, null));
                        int count = 0;
                        Object v = hintsMap.get(pidKey);
                        if (v instanceof Number n) count = n.intValue();
                        else if (v instanceof String s) try { count = Integer.parseInt(s); } catch (Exception ignored) {}
                        if (puzzleId != null) p.setHintCount(puzzleId, Math.max(0, count));
                    }
                }

                // current puzzle id
                String cp = Objects.toString(pObj.get("currentPuzzleId"), null);
                if (cp != null) p.setCurrentPuzzleId(parseUuid(cp));

                user.putProgress(roomId, p);
            }
        }

        // current room hint
        String currentRoomId = Objects.toString(u.get("currentRoomId"), null);
        if (currentRoomId != null) {
            // we will map this to an actual Room when applying progress to a Game
            // Store temporarily via a synthetic progress entry; apply method will use it
            // We keep it accessible via a transient field-like map here if needed
        }

        return user;
    }

    private JSONObject toJsonUser(User user) {
        JSONObject u = new JSONObject();
        u.put("id", user.getId() != null ? user.getId().toString() : null);
        u.put("username", user.getUsername());
        u.put("email", user.getEmail());
        Difficulty d = user.getDifficulty() != null ? user.getDifficulty() : Difficulty.MEDIUM;
        u.put("difficulty", d.name());

        // current room id
        String roomIdStr = user.getCurrentRoom() != null && user.getCurrentRoom().getId() != null
                ? user.getCurrentRoom().getId().toString() : null;
        u.put("currentRoomId", roomIdStr);

        // Progress by room
        JSONObject progressRoot = new JSONObject();
        for (Map.Entry<UUID, Progress> e : user.getProgressByRoomId().entrySet()) {
            UUID roomId = e.getKey();
            Progress p = e.getValue();
            if (roomId == null || p == null) continue;

            JSONObject pObj = new JSONObject();

            JSONObject solvedMap = new JSONObject();
            for (Map.Entry<UUID, Boolean> s : p.getPuzzlesSolved().entrySet()) {
                if (s.getKey() != null && s.getValue() != null) {
                    solvedMap.put(s.getKey().toString(), s.getValue());
                }
            }
            pObj.put("puzzlesSolved", solvedMap);

            JSONObject hintsMap = new JSONObject();
            for (Map.Entry<UUID, Integer> h : p.getPuzzleHints().entrySet()) {
                if (h.getKey() != null && h.getValue() != null) {
                    hintsMap.put(h.getKey().toString(), h.getValue());
                }
            }
            pObj.put("puzzleHints", hintsMap);

            pObj.put("currentPuzzleId", p.getCurrentPuzzleId() != null ? p.getCurrentPuzzleId().toString() : null);

            progressRoot.put(roomId.toString(), pObj);
        }
        u.put("progressByRoomId", progressRoot);
        return u;
    }

    private UUID parseUuid(String text) {
        if (text == null || text.isBlank()) return null;
        try { return UUID.fromString(text.trim()); } catch (Exception e) { return null; }
    }

    /**
     * Apply a user's stored progress to a given Game instance.
     * Sets puzzle solved flags, adjusts room hint limits, unlocks reachable rooms, and moves user.
     */
    public void applyUserProgressToGame(User user, Game game) {
        if (user == null || game == null) return;
        Difficulty difficulty = user.getDifficulty() != null ? user.getDifficulty() : Difficulty.MEDIUM;
        int baseHints = difficulty.getHintLimit();

        // default: first room unlocked; subsequent may be locked
        List<Room> rooms = game.getRooms();
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            r.setHintLimit(baseHints);
            if (i == 0) {
                r.setLocked(false);
            }
        }

        // restore per-room progress
        for (Room room : rooms) {
            Progress p = user.getProgress(room.getId());
            if (p == null) continue;
            // solved flags
            for (Puzzle puzzle : room.getPuzzles()) {
                boolean solved = p.isPuzzleSolved(puzzle.getId());
                puzzle.setSolved(solved);
            }
            // hints used -> adjust remaining
            int used = p.getHintCountForRoom(room);
            int remaining = Math.max(0, baseHints - used);
            room.setHintLimit(remaining);
        }

        // unlock rooms sequentially if prior cleared
        for (int i = 1; i < rooms.size(); i++) {
            Room prev = rooms.get(i - 1);
            Room cur = rooms.get(i);
            if (prev.isCleared()) {
                cur.setLocked(false);
            }
        }

        // try to move user to stored room; fallback to first
        // read currentRoomId from stored progress in file if available via first solved/current puzzle mapping
        // Since User doesn't retain that id directly beyond runtime, infer by last room with any unsolved puzzle
        Room target = null;
        // Prefer a room explicitly with an unsolved puzzle as active
        for (Room room : rooms) {
            Progress p = user.getProgress(room.getId());
            if (p != null && p.getCurrentPuzzleId() != null) {
                target = room;
                break;
            }
        }
        if (target == null) {
            // choose first room not cleared, else last
            for (Room room : rooms) {
                if (!room.isCleared()) { target = room; break; }
            }
        }
        if (target == null && !rooms.isEmpty()) target = rooms.get(0);
        if (target != null) user.moveTo(target);
    }

    /**
     * Capture the current game state back into the user's progress structures.
     */
    public void captureUserProgressFromGame(User user, Game game) {
        if (user == null || game == null) return;
        Difficulty difficulty = user.getDifficulty() != null ? user.getDifficulty() : Difficulty.MEDIUM;
        int baseHints = difficulty.getHintLimit();

        for (Room room : game.getRooms()) {
            Progress p = user.getOrCreateProgress(room.getId());
            // puzzles solved
            for (Puzzle puzzle : room.getPuzzles()) {
                p.setPuzzleSolved(puzzle.getId(), puzzle.isSolved());
            }
            // derive hints used from remaining
            int remaining = Math.max(0, room.getHintLimit());
            int used = Math.max(0, baseHints - remaining);
            for (Puzzle puzzle : room.getPuzzles()) {
                // evenly attribute all used hints to the first puzzle (simple heuristic)
                // or store as total on each puzzle's counter if not set
                if (p.getHintsUsed(puzzle.getId()) == 0) {
                    p.setHintCount(puzzle.getId(), used);
                    break;
                }
            }
            // set current puzzle as first unsolved in this room
            UUID cur = null;
            for (Puzzle puzzle : room.getPuzzles()) {
                if (!puzzle.isSolved()) { cur = puzzle.getId(); break; }
            }
            p.setCurrentPuzzleId(cur);
        }
    }
}

