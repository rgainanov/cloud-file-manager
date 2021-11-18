package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestGoUp extends Message{
    private String currentDir;

    public ClientRequestGoUp(String currentDir) {
        this.currentDir = currentDir;
    }
}
