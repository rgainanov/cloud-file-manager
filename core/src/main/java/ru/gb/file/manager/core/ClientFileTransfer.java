package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientFileTransfer extends Message {
    private String filePath;
    private byte[] file;

    public ClientFileTransfer(String filePath, byte[] file) {
        this.filePath = filePath;
        this.file = file;
    }
}
