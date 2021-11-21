package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientFileTransfer implements Message {
    private String filePath;
    private byte[] file;

    public ClientFileTransfer(String filePath, byte[] file) {
        this.filePath = filePath;
        this.file = file;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_FILE_TRANSFER;
    }
}
