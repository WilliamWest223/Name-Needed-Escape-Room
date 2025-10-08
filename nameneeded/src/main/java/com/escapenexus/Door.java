package com.escapenexus;

import java.util.UUID;

public class Door {
    private final UUID id;
    private String name;
    private String description;
    private final UUID fromRoomId;
    private final UUID toRoomId;
    private boolean locked = true;
    private boolean open = false;
    private Item keyRequired;

    public Door(String name, String description, UUID fromRoomId, UUID toRoomId) {
        this(null, name, description, fromRoomId, toRoomId);
    }

    public Door(UUID id, String name, String description, UUID fromRoomId, UUID toRoomId) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.fromRoomId = fromRoomId;
        this.toRoomId = toRoomId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UUID getFromRoomId() { return fromRoomId; }
    public UUID getToRoomId() { return toRoomId; }
    public boolean isLocked() { return locked; }
    public boolean isOpen() { return open; }
    public Item getKeyRequired() { return keyRequired; }
    public void setKeyRequired(Item keyRequired) { this.keyRequired = keyRequired; }

    public boolean unlock(Item key) {
        if (!locked) return false;
        if (keyRequired == null || key == null) return false;
        if (keyRequired.getId().equals(key.getId())) {
            locked = false;
            return true;
        }
        return false;
    }

    public boolean open() {
        if (locked) return false;
        open = true;
        return true;
    }

    public void close() {
        open = false;
    }
}
