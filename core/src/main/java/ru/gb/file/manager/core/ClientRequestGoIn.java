package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestGoIn extends Message{
    private String currentDir;
    private String fileName;

    public ClientRequestGoIn(String currentDir, String fileName) {
        this.currentDir = currentDir;
        this.fileName = fileName;
    }
}
