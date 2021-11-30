package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ClientRequestFileShare implements Message{
    private String login;
    private String filePath;

    public ClientRequestFileShare(String login, String filePath) {
        this.login = login;
        this.filePath = filePath;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.CLIENT_REQUEST_FILE_SHARE;
    }
}
