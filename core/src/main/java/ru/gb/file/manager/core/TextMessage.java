package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class TextMessage extends Message {
    private String msg;

    public TextMessage(String msg) {
        this.msg = msg;
    }
}

