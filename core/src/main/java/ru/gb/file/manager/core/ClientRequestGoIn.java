package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestGoIn implements Message{
    private String currentDir;
    private String fileName;

    public ClientRequestGoIn(String currentDir, String fileName) {
        this.currentDir = currentDir;
        this.fileName = fileName;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_PATH_GO_IN;
    }
}
