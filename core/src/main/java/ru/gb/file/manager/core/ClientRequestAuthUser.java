package ru.gb.file.manager.core;

import lombok.Data;
import sun.security.rsa.RSAKeyPairGenerator;

@Data
public class ClientRequestAuthUser implements Message {
    private String login;
    private String password;
    private boolean newUser;

    public ClientRequestAuthUser(String login, String password) {
        this.login = login;
        this.password = password;
        this.newUser = false;
    }

    public ClientRequestAuthUser(String login, String password, boolean newUser) {
        this.login = login;
        this.password = password;
        this.newUser = newUser;
    }

    private String encrypt(String password) {
        return null;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.AUTH_REQUEST;
    }
}
