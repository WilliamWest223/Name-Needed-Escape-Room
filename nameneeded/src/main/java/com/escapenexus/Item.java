package com.escapenexus;

import java.util.Objects;
import java.util.UUID;

public class Item {

    private final UUID id;
    private String name;
    private String description;
    private boolean portable;
    private boolean key;
    private ItemState state;

    public Item(String name, String description, boolean portable, boolean key, ItemState state) {
        this(null, name, description, portable, key, state);
    }

    public Item(UUID id, String name, String description, boolean portable, boolean key, ItemState state) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.portable = portable;
        this.key = key;
        this.state = state != null ? state : ItemState.NEW;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPortable() {
        return portable;
    }

    public void setPortable(boolean portable) {
        this.portable = portable;
    }

    public boolean isKey() {
        return key;
    }

    public void setKey(boolean key) {
        this.key = key;
    }

    public ItemState getState() {
        return state;
    }

    public void setState(ItemState state) {
        this.state = state != null ? state : ItemState.NEW;
    }

    public boolean use(User user, Object target) {
        return false;
    }

    public String inspect() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item item)) {
            return false;
        }
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
