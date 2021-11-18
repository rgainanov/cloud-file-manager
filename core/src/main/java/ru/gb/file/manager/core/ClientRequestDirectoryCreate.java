package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestDirectoryCreate extends Message {
    private String newDir;

    public ClientRequestDirectoryCreate(String dirName) {
        this.newDir = dirName;
    }
}
