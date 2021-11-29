package ru.gb.file.manager.core;

import lombok.Data;

@Data
public class FileTransfer implements Message{
    private String fileName;
    private String filePath;
    private long batchCount;
    private int currentBatch;
    private int batchLength;
    private byte[] filePart;


    public FileTransfer(String fileName, String filePath, long batchCount, int currentBatch, int batchLength, byte[] filePart) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.batchCount = batchCount;
        this.currentBatch = currentBatch;
        this.batchLength = batchLength;
        this.filePart = filePart;
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.FILE_TRANSFER;
    }
}
