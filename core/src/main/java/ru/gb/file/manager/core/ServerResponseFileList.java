package ru.gb.file.manager.core;

import lombok.Data;

import java.util.List;

@Data
public class ServerResponseFileList extends Message{
    private String serverPath;
    private List<FileModel> list;

    public ServerResponseFileList(List<FileModel> list, String serverPath) {
        this.list = list;
        this.serverPath = serverPath;
    }
}
