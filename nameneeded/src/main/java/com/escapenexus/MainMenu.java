package com.escapenexus;

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
            System.out.println("2) Load Game");
            System.out.println("3) Instructions");
            System.out.println("4) Quit");
            System.out.print("Select an option: ");

            String line = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter 1-4.");
                continue;
            }

            switch (choice) {
                case 1 -> manager.startNewGame();
                case 2 -> manager.loadGame();
                case 3 -> manager.showInstructions();
                case 4 -> { manager.exitGame(); running = false; }
                default -> System.out.println("Please enter 1-4.");
            }
        }
    }
}
