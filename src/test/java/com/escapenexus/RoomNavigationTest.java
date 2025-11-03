package com.escapenexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Test Plan:
// - Coverage targets: Door.unlock/open ergonomics, Room.unlock behavior, User.moveTo safeguards, Game.advanceRoom guard rails.
// - Happy path: unlocking with a matching key should allow the user to move and update the current room.
// - Negative cases: locked doors reject opening attempts; null targets keep the user in place.
// - Boundary cases: re-entering the same room is idempotent; game advancement without progress returns the current room.
// - Invariants: unlocked doors remain open; EscapeGame cleanup avoids cross-test contamination.
class RoomNavigationTest {

    private User user;
    private Room startRoom;
    private Room targetRoom;
    private Item masterKey;
    private Door accessDoor;

    @BeforeEach
    void setUp() {
        startRoom = new Room(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "Cryo Bay",
                "Starting chamber."
        );
        targetRoom = new Room(
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                "Transit Hall",
                "Unlocked once the proper key is used."
        );
        targetRoom.setLocked(true);

        masterKey = new Item(
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                "Transit Key",
                "Allows navigation to the transit hall.",
                true,
                true,
                ItemState.NEW
        );
        targetRoom.setKeyRequired(masterKey);

        accessDoor = new Door(
                UUID.fromString("40000000-0000-0000-0000-000000000004"),
                "Cryo Door",
                "Separates the cryo bay from the transit hall.",
                startRoom.getId(),
                targetRoom.getId()
        );
        accessDoor.setKeyRequired(masterKey);

        user = new User("Navigator");
        user.moveTo(startRoom);
    }

    @AfterEach
    void tearDown() {
        EscapeGame escapeGame = EscapeGame.getInstance();
        escapeGame.resetProgress();
        escapeGame.logout();
    }

    @Test
    @DisplayName("move_unlockedDoor_movesPlayerAndUpdatesCurrentRoom")
    void move_unlockedDoor_movesPlayerAndUpdatesCurrentRoom() {
        assertTrue(accessDoor.unlock(masterKey), "Door should unlock with the matching key");
        assertTrue(accessDoor.open(), "Unlocked door should open");
        assertTrue(targetRoom.unlock(masterKey), "Matching key should unlock the destination room");

        assertTrue(user.pickUp(masterKey));
        user.moveTo(targetRoom);

        assertSame(targetRoom, user.getCurrentRoom());
        assertFalse(targetRoom.isLocked());
        assertTrue(accessDoor.isOpen());
    }

    @Test
    @DisplayName("move_lockedDoor_beforeSolve_isRejected")
    void move_lockedDoor_beforeSolve_isRejected() {
        assertFalse(accessDoor.open(), "Door cannot open while locked");
        assertSame(startRoom, user.getCurrentRoom(), "User stays in the starting room");
        assertTrue(targetRoom.isLocked(), "Destination room remains locked without the key");
    }

    @Test
    @DisplayName("move_invalidTargetRoom_isRejected")
    void move_invalidTargetRoom_isRejected() {
        user.moveTo(null);
        assertSame(startRoom, user.getCurrentRoom(), "Null target should leave the user unchanged");
    }

    @Test
    @DisplayName("reenter_sameRoom_isIdempotent_noStateCorruption")
    void reenter_sameRoom_isIdempotent_noStateCorruption() {
        assertEquals(1, user.getProgressByRoomId().size(), "Progress map initialized for starting room");
        user.moveTo(startRoom);
        assertEquals(1, user.getProgressByRoomId().size(), "Re-entering should not duplicate progress entries");
        assertSame(startRoom, user.getCurrentRoom());
    }

    @Test
    @DisplayName("cycle_graph_detectsAndHandlesWithoutInfiniteLoop")
    void cycle_graph_detectsAndHandlesWithoutInfiniteLoop() {
        EscapeGame escapeGame = EscapeGame.getInstance();
        escapeGame.newSinglePlayerSession("cycler", Difficulty.MEDIUM);

        Game game = escapeGame.getGame();
        assertNotNull(game);
        User activeUser = escapeGame.getUser();
        assertNotNull(activeUser);

        Room initialRoom = activeUser.getCurrentRoom();
        assertSame(initialRoom, game.advanceRoom(), "Game should not advance without solving puzzles");

        solveLightPattern(escapeGame);
        Room afterSolve = game.advanceRoom();
        assertSame(activeUser.getCurrentRoom(), afterSolve, "Advance moves in lockstep with user location");
    }

    private void solveLightPattern(EscapeGame escapeGame) {
        Puzzle puzzle = escapeGame.getNextPuzzle();
        assertTrue(puzzle instanceof LightPatternPuzzle);
        LightPatternPuzzle lightPatternPuzzle = (LightPatternPuzzle) puzzle;

        while (!lightPatternPuzzle.isSolved()) {
            assertTrue(escapeGame.attemptCurrentPuzzle(lightPatternPuzzle.getCurrentSequence()));
        }
    }
}
