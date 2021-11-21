package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ServerResponseTextMessage implements Message {
    private String msg;

    public ServerResponseTextMessage(String msg) {
        this.msg = msg;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.SERVER_RESPONSE_TXT_MESSAGE;
    }
}

