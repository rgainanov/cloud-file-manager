package ru.gb.file.manager.server;

public interface AuthProvider {
    void start();

    void stop();

    String[] getUsers(String login);

    boolean addUserRecord(String login, String password);
}
