package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestFileCreate implements Message {
    private String fileName;
    private String filePath;

    public ClientRequestFileCreate(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_TOUCH;
    }
}
