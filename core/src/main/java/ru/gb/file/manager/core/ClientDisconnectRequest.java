package ru.gb.file.manager.core;

public class ClientDisconnectRequest implements Message {
    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_DISCONNECT_REQUEST;
    }
}
