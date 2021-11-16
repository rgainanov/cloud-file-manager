package ru.gb.file.manager.core;

import lombok.Data;

import java.util.List;

@Data
public class ServerFileList extends Message{
    private String serverPath;
    private List<FileModel> list;

    public ServerFileList(List<FileModel> list, String serverPath) {
        this.list = list;
        this.serverPath = serverPath;
    }
}
