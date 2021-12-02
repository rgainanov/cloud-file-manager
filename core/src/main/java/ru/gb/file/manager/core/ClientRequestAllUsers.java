package ru.gb.file.manager.core;

public class ClientRequestAllUsers implements Message{
    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_ALL_USERS;
    }
}
