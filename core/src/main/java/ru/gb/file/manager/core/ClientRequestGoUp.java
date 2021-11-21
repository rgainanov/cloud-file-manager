package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestGoUp implements Message{
    private String currentDir;

    public ClientRequestGoUp(String currentDir) {
        this.currentDir = currentDir;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_PATH_GO_UP;
    }
}
