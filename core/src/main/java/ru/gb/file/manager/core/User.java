package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class User extends Message {
    private String login;
    private String password;
    private boolean newUser;

    public User(String login, String password) {
        this.login = login;
        this.password = password;
        this.newUser = false;
    }

    public User(String login, String password, boolean newUser) {
        this.login = login;
        this.password = password;
        this.newUser = newUser;
    }
}
