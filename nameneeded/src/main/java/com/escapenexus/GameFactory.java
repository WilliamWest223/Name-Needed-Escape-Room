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

        LightPatternPuzzle rebootSequence = new LightPatternPuzzle(
                UUID.fromString("bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1"),
                "Reboot Sequence",
                "Replay each flashing sequence on the cryo console.",
                System.nanoTime()
        );
        rebootSequence.addHint("Watch carefully - each new flash extends the full sequence.");
        rebootSequence.addHint("Group colors in pairs or triples to memorize faster.");
        rebootSequence.addHint("Say each color out loud as you replay it.");
        rebootSequence.setKeyProvided(keyCryoToHall);
        cryoIntake.addPuzzle(rebootSequence);

        Room transitHall = new Room(
                UUID.fromString("aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2"),
                "Transit Hall",
                "Navigate the evacuated hall and bypass the mag-lock pattern wall."
        );
        transitHall.setLocked(true);
        transitHall.setKeyRequired(keyCryoToHall);

        RiddlePuzzle magLockRiddle = new RiddlePuzzle(
                UUID.fromString("bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2"),
                "Mag-Lock Riddle",
                "I have a bed but do not sleep, a mouth but do not speak, and a foot but do not walk. What am I?",
                java.util.List.of("river")
        );
        magLockRiddle.addHint("It moves yet stays in its place.");
        magLockRiddle.addHint("Its mouth isn't for speaking.");
        magLockRiddle.addHint("You can follow its banks.");
        magLockRiddle.setKeyProvided(keyHallToCore);
        transitHall.addPuzzle(magLockRiddle);

        Room coreVault = new Room(
                UUID.fromString("aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3"),
                "Core Vault",
                "Align the reactor and vent energy before the singularity stabilizes."
        );
        coreVault.setLocked(true);
        coreVault.setKeyRequired(keyHallToCore);

        MathPuzzle reactorAlignment = new MathPuzzle(
                UUID.fromString("bbbbbbb3-bbbb-bbbb-bbbb-bbbbbbbbbbb3"),
                "Reactor Calibration",
                "Two calibration panels display 5 and 7. Enter their sum to confirm alignment.",
                12.0,
                0.0
        );
        reactorAlignment.addHint("Add the two numbers you see.");
        reactorAlignment.addHint("It's simple arithmetic, not degrees or timing.");
        reactorAlignment.addHint("Five plus seven equals...?");
        coreVault.addPuzzle(reactorAlignment);

        game.addRoom(cryoIntake);
        game.addRoom(transitHall);
        game.addRoom(coreVault);

        for (Room room : game.getRooms()) {
            room.setHintLimit(resolvedDifficulty.getHintLimit());
        }

        return game;
    }
}
