package com.escapenexus;

import java.time.Duration;
import java.util.UUID;

public final class GameFactory {

    private GameFactory() {
    }

    public static Game createDefaultThreeRoomGame(Difficulty difficulty) {
        Difficulty resolvedDifficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        Game game = new Game(
                "Escape Nexus: Singularity Run",
                "Race through the facility before the singularity ignites.",
                resolvedDifficulty,
                30,
                1
        );
        game.setDifficulty(resolvedDifficulty);
        game.setTimeLimit(Duration.ofMinutes(30));

        Item keyCryoToHall = new Item(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Cryo Bay Access Key",
                "Unlocks the transit hall mag-lock.",
                true,
                true,
                ItemState.NEW
        );

        Item keyHallToCore = new Item(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Core Vault Key",
                "Unlocks the reactor core vault.",
                true,
                true,
                ItemState.NEW
        );

        game.addItem(keyCryoToHall);
        game.addItem(keyHallToCore);

        Room cryoIntake = new Room(
                UUID.fromString("aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1"),
                "Cryo Intake",
                "Wake up and stabilize the cryo systems before sprinting out."
        );
        cryoIntake.setLocked(false);

        Puzzle rebootSequence = new Puzzle(
                UUID.fromString("bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1"),
                "Reboot Sequence",
                "Restart the cryo control cluster without tripping failsafes."
        );
        rebootSequence.addHint("Initiate diagnostics before rebooting anything.");
        rebootSequence.addHint("Power cycles must follow the blue-green-orange order.");
        rebootSequence.addHint("Pair the thermal reset with the pressure equalizer toggle.");
        rebootSequence.addHint("The last step is a manual override on the console.");
        rebootSequence.setKeyProvided(keyCryoToHall);
        cryoIntake.addPuzzle(rebootSequence);

        Room transitHall = new Room(
                UUID.fromString("aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2"),
                "Transit Hall",
                "Navigate the evacuated hall and bypass the mag-lock pattern wall."
        );
        transitHall.setLocked(true);
        transitHall.setKeyRequired(keyCryoToHall);

        Puzzle magLockPattern = new Puzzle(
                UUID.fromString("bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2"),
                "Mag-Lock Pattern",
                "Decode the alternating magnetic locks before power surges."
        );
        magLockPattern.addHint("Observe the pulsing lights: they echo the safe pattern.");
        magLockPattern.addHint("Odd-numbered locks cycle clockwise.");
        magLockPattern.addHint("Reset any lock that pulses red twice.");
        magLockPattern.addHint("The final sequence is blue, blue, green, amber.");
        magLockPattern.setKeyProvided(keyHallToCore);
        transitHall.addPuzzle(magLockPattern);

        Room coreVault = new Room(
                UUID.fromString("aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3"),
                "Core Vault",
                "Align the reactor and vent energy before the singularity stabilizes."
        );
        coreVault.setLocked(true);
        coreVault.setKeyRequired(keyHallToCore);

        Puzzle reactorAlignment = new Puzzle(
                UUID.fromString("bbbbbbb3-bbbb-bbbb-bbbb-bbbbbbbbbbb3"),
                "Reactor Alignment",
                "Synchronize the quantum regulators and field dampers."
        );
        reactorAlignment.addHint("Lock the primary coolant rings first.");
        reactorAlignment.addHint("Align the regulator phases to match the holographic display.");
        reactorAlignment.addHint("Stabilize the output by venting the upper conduits.");
        reactorAlignment.addHint("Seal the chamber by engaging the twin dampers simultaneously.");
        coreVault.addPuzzle(reactorAlignment);

        game.addRoom(cryoIntake);
        game.addRoom(transitHall);
        game.addRoom(coreVault);

        return game;
    }
}
