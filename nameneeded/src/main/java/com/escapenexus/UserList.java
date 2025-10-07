package com.escapenexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class UserList {

    private static UserList instance;
    private final List<User> users = new ArrayList<>();

    private UserList() {
    }

    public static synchronized UserList getInstance() {
        if (instance == null) {
            instance = new UserList();
        }
        return instance;
    }

    public User addUser(String username, String password, String email) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        User user = new User(UUID.randomUUID(), username, email);
        users.add(user);
        return user;
    }

    public boolean removeUser(String username) {
        if (username == null) {
            return false;
        }
        return users.removeIf(user -> username.equalsIgnoreCase(user.getUsername()));
    }

    public List<User> getAllUsers() {
        return Collections.unmodifiableList(users);
    }

    public Optional<User> getUser(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return users.stream()
                .filter(user -> username.equalsIgnoreCase(user.getUsername()))
                .findFirst();
    }

    public void save() {
    }
}
