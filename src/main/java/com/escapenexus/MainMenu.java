package com.escapenexus;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MainMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final GameManager manager;
    private boolean running = true;

    public MainMenu(GameManager manager) {
        this.manager = manager;
    }

    public void run() {
        while (running) {
            System.out.println();
            System.out.println("=== Escape Room ===");
            System.out.println("1) New Game");
            System.out.println("2) Load Game (default)");
            System.out.println("3) Instructions");
            System.out.println("4) Save Game");
            System.out.println("5) Load Save (from file)");
            System.out.println("6) Quit");
            System.out.print("Select an option: ");

            String line = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter 1-6.");
                continue;
            }

            switch (choice) {
                case 1 -> manager.startNewGame();
                case 2 -> manager.loadGame();
                case 3 -> manager.showInstructions();
                case 4 -> handleSaveGame();
                case 5 -> handleLoadSave();
                case 6 -> { manager.exitGame(); running = false; }
                default -> System.out.println("Please enter 1-6.");
            }
        }
    }

    private void handleSaveGame() {
        System.out.println("Save to file (leave blank for default saves/current-game.json):");
        System.out.print("> ");
        String path = scanner.nextLine().trim();
        try {
            if (path.isEmpty()) {
                manager.saveCurrentGame();
            } else {
                Path file = Paths.get(path);
                manager.saveCurrentGame(file);
            }
            System.out.println("Game saved.");
        } catch (Exception e) {
            System.out.println("Failed to save: " + e.getMessage());
        }
    }

    private void handleLoadSave() {
        System.out.println("Load from file (leave blank for default saves/current-game.json):");
        System.out.print("> ");
        String path = scanner.nextLine().trim();
        try {
            if (path.isEmpty()) {
                manager.loadGame();
            } else {
                Path file = Paths.get(path);
                manager.loadGame(file);
            }
            System.out.println("Game loaded.");
        } catch (Exception e) {
            System.out.println("Failed to load: " + e.getMessage());
        }
    }
}
