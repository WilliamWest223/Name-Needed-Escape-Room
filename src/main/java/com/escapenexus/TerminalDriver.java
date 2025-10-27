package com.escapenexus;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class TerminalDriver {

    private static final Scanner INPUT = new Scanner(System.in);

    private TerminalDriver() {
    }

    public static void main(String[] args) {
        GameManager manager = new GameManager();
        UserStorage userStorage = new UserStorage();
        UserList userList = UserList.getInstance();
        userList.load();

        // Login / select user
        User user = loginFlow(userList);
        if (user == null) return;

        Difficulty difficulty = user.getDifficulty() != null ? user.getDifficulty() : Difficulty.MEDIUM;
        Game game = manager.startNewGame(difficulty);
        for (Room room : game.getRooms()) {
            room.setHintLimit(difficulty.getHintLimit());
        }
        userStorage.applyUserProgressToGame(user, game);
        if (user.getCurrentRoom() == null && !game.getRooms().isEmpty()) {
            user.moveTo(game.getRooms().get(0));
        }

        System.out.println("== Escape Nexus (Terminal) ==");
        System.out.println("User: " + user.getUsername() + "  | Difficulty: " + difficulty.name());
        while (true) {
            Room room = user.getCurrentRoom();
            if (room == null) {
                return;
            }

            Puzzle puzzle = room.getPuzzles().get(0);
            System.out.println();
            System.out.println("Room: " + room.getName() + "  | Hints left: " + room.getHintLimit());
            System.out.println("Puzzle: " + puzzle.getTitle());
            System.out.println(puzzle.getDescription());
            System.out.print("[1] hint  [2] attempt  ");
            if (puzzle instanceof LightPatternPuzzle) {
                System.out.print("[3] show-seq  ");
            }
            System.out.println("[4] save  [5] load  [6] switch-user  [0] quit");
            System.out.print("> ");

            String choice = INPUT.nextLine().trim();
            switch (choice) {
                case "1" -> handleHint(room, puzzle);
                case "2" -> handleAttempt(game, user, room, puzzle);
                case "3" -> handleSequence(puzzle);
                case "4" -> {
                    userStorage.captureUserProgressFromGame(user, game);
                    userList.addOrReplace(user);
                    userList.save();
                    System.out.println("-> saved user progress");
                }
                case "5" -> game = handleLoad(manager, difficulty, user, game);
                case "6" -> {
                    // Save current progress first
                    userStorage.captureUserProgressFromGame(user, game);
                    userList.addOrReplace(user);
                    userList.save();
                    // Switch
                    User newUser = loginFlow(userList);
                    if (newUser != null) {
                        user = newUser;
                        Difficulty nd = user.getDifficulty() != null ? user.getDifficulty() : Difficulty.MEDIUM;
                        game = manager.startNewGame(nd);
                        for (Room r : game.getRooms()) { r.setHintLimit(nd.getHintLimit()); }
                        userStorage.applyUserProgressToGame(user, game);
                        if (user.getCurrentRoom() == null && !game.getRooms().isEmpty()) {
                            user.moveTo(game.getRooms().get(0));
                        }
                        System.out.println("-> switched to user: " + user.getUsername());
                    }
                }
                case "0" -> {
                    // on exit, persist current progress
                    try {
                        userStorage.captureUserProgressFromGame(user, game);
                        userList.addOrReplace(user);
                        userList.save();
                    } catch (Exception ignored) {}
                    return;
                }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private static User loginFlow(UserList userList) {
        while (true) {
            List<User> users = new ArrayList<>(userList.getAllUsers());
            System.out.println();
            System.out.println("== Login ==");
            if (users.isEmpty()) {
                System.out.println("No users found. Create new.");
                return createUser(userList);
            }
            System.out.println("Select user or create new:");
            for (int i = 0; i < users.size(); i++) {
                System.out.println("[" + (i + 1) + "] " + users.get(i).getUsername());
            }
            System.out.println("[N] New user");
            System.out.print("> ");
            String input = INPUT.nextLine().trim();
            if (input.equalsIgnoreCase("n")) {
                return createUser(userList);
            }
            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < users.size()) {
                    return users.get(idx);
                }
            } catch (NumberFormatException ignored) { }
            System.out.println("Please enter a valid option.");
        }
    }

    private static User createUser(UserList userList) {
        System.out.println("Enter username:");
        System.out.print("> ");
        String username = INPUT.nextLine().trim();
        if (username.isBlank()) {
            System.out.println("Username is required.");
            return null;
        }
        Difficulty difficulty = promptDifficulty();
        User user = userList.addUser(username, null, "");
        user.setDifficulty(difficulty);
        userList.save();
        return user;
    }

    private static Difficulty promptDifficulty() {
        while (true) {
            System.out.println("Choose difficulty: [E]asy (3 hints), [M]edium (2 hints), [H]ard (1 hint)");
            System.out.print("> ");
            String input = INPUT.nextLine().trim().toUpperCase();
            if (input.isEmpty()) {
                continue;
            }
            char choice = input.charAt(0);
            switch (choice) {
                case 'E':
                    return Difficulty.EASY;
                case 'M':
                    return Difficulty.MEDIUM;
                case 'H':
                    return Difficulty.HARD;
                default:
                    System.out.println("Please enter E, M, or H.");
            }
        }
    }

    private static void handleHint(Room room, Puzzle puzzle) {
        if (room.getHintLimit() <= 0) {
            System.out.println("HINT: No hints left.");
        } else {
            System.out.println("HINT: " + puzzle.giveHint());
            room.setHintLimit(room.getHintLimit() - 1);
        }
    }

    private static void handleAttempt(Game game, User user, Room room, Puzzle puzzle) {
        boolean success = attemptPuzzle(puzzle);
        System.out.println(success ? "✔ success" : "✘ fail");
        if (!puzzle.isSolved()) {
            return;
        }

        Item key = puzzle.getKeyProvided();
        if (key != null) {
            int index = game.getRooms().indexOf(room);
            if (index + 1 < game.getRooms().size()) {
                game.getRooms().get(index + 1).unlock(key);
            }
        }

        if (!room.isCleared()) {
            return;
        }

        int index = game.getRooms().indexOf(room);
        if (index + 1 < game.getRooms().size()) {
            user.moveTo(game.getRooms().get(index + 1));
            System.out.println("-> advanced to next room");
        } else {
            System.out.println("All rooms cleared!");
            System.exit(0);
        }
    }

    private static void handleSequence(Puzzle puzzle) {
        if (puzzle instanceof LightPatternPuzzle lightPattern) {
            System.out.println("Sequence: " + lightPattern.getCurrentSequence());
        } else {
            System.out.println("No sequence to show.");
        }
    }

    private static void handleSave(GameManager manager) {
        System.out.println("Save to file (leave blank for default saves/current-game.json):");
        System.out.print("> ");
        String path = INPUT.nextLine().trim();
        try {
            if (path.isEmpty()) {
                manager.saveCurrentGame();
            } else {
                java.nio.file.Path file = java.nio.file.Paths.get(path);
                manager.saveCurrentGame(file);
            }
            System.out.println("-> saved game");
        } catch (Exception e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private static Game handleLoad(GameManager manager, Difficulty difficulty, User user, Game current) {
        System.out.println("Load from file (leave blank for default saves/current-game.json):");
        System.out.print("> ");
        String path = INPUT.nextLine().trim();
        try {
            Game game;
            if (path.isEmpty()) {
                game = manager.loadGame();
            } else {
                java.nio.file.Path file = java.nio.file.Paths.get(path);
                game = manager.loadGame(file);
            }
            for (Room r : game.getRooms()) {
                r.setHintLimit(difficulty.getHintLimit());
            }
            if (!game.getRooms().isEmpty()) {
                user.moveTo(game.getRooms().get(0));
            }
            System.out.println("-> loaded game");
            return game;
        } catch (Exception e) {
            System.out.println("Load failed: " + e.getMessage());
            return current;
        }
    }

    private static boolean attemptPuzzle(Puzzle puzzle) {
        if (puzzle instanceof LightPatternPuzzle) {
            System.out.println("Enter colors (e.g., RED GREEN BLUE):");
            String[] tokens = INPUT.nextLine().trim().split("\\s+");
            List<LightColor> guess = new ArrayList<>();
            try {
                for (String token : tokens) {
                    guess.add(LightColor.fromString(token));
                }
            } catch (Exception exception) {
                System.out.println("Bad color.");
                return false;
            }
            return puzzle.attempt(guess);
        } else if (puzzle instanceof RiddlePuzzle) {
            System.out.println("Answer (single word):");
            return puzzle.attempt(INPUT.nextLine());
        } else if (puzzle instanceof MathPuzzle) {
            System.out.println("Enter number:");
            return puzzle.attempt(INPUT.nextLine());
        }

        System.out.println("Enter attempt:");
        return puzzle.attempt(INPUT.nextLine());
    }
}
