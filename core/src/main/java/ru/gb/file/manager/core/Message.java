package ru.gb.file.manager.core;

import java.io.Serializable;

public interface Message extends Serializable {
    CommandTypes getType();
}
