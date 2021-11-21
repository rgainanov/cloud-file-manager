package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ServerResponseFile implements Message{
    private String fileName;
    private byte[] file;

    public ServerResponseFile(String fileName, byte[] file) {
        this.fileName = fileName;
        this.file = file;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.SERVER_RESPONSE_FILE;
    }
}
