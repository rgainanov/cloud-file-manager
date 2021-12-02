package ru.gb.file.manager.server;

import java.util.List;

public interface AuthProvider {
    void start();

    void stop();

    String[] getUsers(String login);

    List<String> getAllUsers();

    boolean addUserRecord(String login, String password);
}
