package ru.gb.file.manager.core;

import lombok.Data;

import java.nio.file.Path;

@Data
public class ClientRequestFile implements Message {
    private String filePath;

    public ClientRequestFile(Path filePath) {
        this.filePath = filePath.toString();
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_FILE_REQUEST;
    }
}
