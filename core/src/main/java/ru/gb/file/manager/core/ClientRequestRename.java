package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestRename implements Message{
    private String currentPath;
    private String newPath;

    public ClientRequestRename(String currentPath, String newPath) {
        this.currentPath = currentPath;
        this.newPath = newPath;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_RENAME;
    }
}
