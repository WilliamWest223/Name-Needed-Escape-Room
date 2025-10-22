package com.escapenexus;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class TerminalDriver {

    private static final Scanner INPUT = new Scanner(System.in);

    private TerminalDriver() {
    }

    public static void main(String[] args) {
        Difficulty difficulty = promptDifficulty();
        Game game = GameFactory.createDefaultThreeRoomGame(difficulty);
        User user = new User("player");
        user.setDifficulty(difficulty);
        for (Room room : game.getRooms()) {
            room.setHintLimit(difficulty.getHintLimit());
        }
        user.moveTo(game.getRooms().get(0));

        System.out.println("== Escape Nexus (Terminal) ==");
        System.out.println("Difficulty: " + difficulty.name());
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
            System.out.println("[0] quit");
            System.out.print("> ");

            String choice = INPUT.nextLine().trim();
            switch (choice) {
                case "1" -> handleHint(room, puzzle);
                case "2" -> handleAttempt(game, user, room, puzzle);
                case "3" -> handleSequence(puzzle);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Unknown option.");
            }
        }
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
