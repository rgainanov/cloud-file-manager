package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class ServerResponseFile implements Message{
    private String fileName;
    private long batchCount;
    private int currentBatch;
    private int batchLength;
    private byte[] filePart;

//    public ServerResponseFile() {
//    }

    public ServerResponseFile(String fileName, long batchCount, int currentBatch, int batchLength, byte[] filePart) {
        this.fileName = fileName;
        this.batchCount = batchCount;
        this.currentBatch = currentBatch;
        this.batchLength = batchLength;
        this.filePart = filePart;
    }

    //
//    public ServerResponseFile(String fileName, byte[] file) {
//        this.fileName = fileName;
//        this.file = file;
//    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.SERVER_RESPONSE_FILE;
    }
}
