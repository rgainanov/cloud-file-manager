package ru.gb.file.manager.core;

import lombok.Data;

import java.util.List;

@Data
public class ServerResponseRegisteredUsers implements Message{
    private List<String> registeredUsers;

    public ServerResponseRegisteredUsers(List<String> registeredUsers) {
        this.registeredUsers = registeredUsers;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.SERVER_RESPONSE_REGISTERED_USERS;
    }
}
