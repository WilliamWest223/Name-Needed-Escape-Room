package com.escapenexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Inventory {

    private final List<Item> items = new ArrayList<>();

    public boolean add(Item item) {
        if (item == null) {
            return false;
        }
        if (items.contains(item)) {
            return false;
        }
        return items.add(item);
    }

    public boolean remove(Item item) {
        if (item == null) {
            return false;
        }
        return items.remove(item);
    }

    public boolean contains(Item item) {
        return items.contains(item);
    }

    public Item findById(UUID id) {
        if (id == null) {
            return null;
        }
        return items.stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void clear() {
        items.clear();
    }
}
