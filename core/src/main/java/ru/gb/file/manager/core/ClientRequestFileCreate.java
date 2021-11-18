package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestFileCreate extends Message {
    private String fileName;
    private String filePath;

    public ClientRequestFileCreate(String fileName) {
        this(fileName, "");
    }

    public ClientRequestFileCreate(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }
}
