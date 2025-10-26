package com.escapenexus;

import java.util.UUID;

public class Player {
    private final UUID id;
    private String name;
    private Room currentRoom;
    private Inventory inventory;
    private int hintsUsed;

    public Player(String name, Room startingRoom) {
        this(null, name, startingRoom);
    }

    public Player(UUID id, String name, Room startingRoom) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.name = name;
        this.currentRoom = startingRoom;
        this.inventory = new Inventory();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room currentRoom) { this.currentRoom = currentRoom; }
    public Inventory getInventory() { return inventory; }
    public int getHintsUsed() { return hintsUsed; }

    public void addItem(Item item) {
        if (item != null) inventory.addItem(item);
    }

    public boolean hasItem(Item item) {
        if (item == null) return false;
        return inventory.contains(item);
    }

    public void useHint() {
        hintsUsed++;
    }
}
