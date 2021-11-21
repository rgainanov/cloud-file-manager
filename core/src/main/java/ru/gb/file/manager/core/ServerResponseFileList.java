package ru.gb.file.manager.core;

import lombok.Data;

import java.util.List;

@Data
public class ServerResponseFileList implements Message{
    private String serverPath;
    private List<FileModel> list;

    public ServerResponseFileList(List<FileModel> list, String serverPath) {
        this.list = list;
        this.serverPath = serverPath;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.SERVER_RESPONSE_FILE_LIST;
    }
}
