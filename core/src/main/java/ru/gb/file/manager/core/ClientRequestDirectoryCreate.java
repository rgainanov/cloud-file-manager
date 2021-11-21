package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestDirectoryCreate implements Message {
    private String newDir;

    public ClientRequestDirectoryCreate(String dirName) {
        this.newDir = dirName;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_MKDIR;
    }
}
