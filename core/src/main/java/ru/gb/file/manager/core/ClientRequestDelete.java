package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestDelete implements Message{
    private String path;

    public ClientRequestDelete(String path) {
        this.path = path;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_DELETE;
    }
}
