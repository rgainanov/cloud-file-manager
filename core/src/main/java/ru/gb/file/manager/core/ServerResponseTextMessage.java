package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ServerResponseTextMessage extends Message {
    private String msg;

    public ServerResponseTextMessage(String msg) {
        this.msg = msg;
    }
}

