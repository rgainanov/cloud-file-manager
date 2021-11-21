package ru.gb.file.manager.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class FileModel implements Message {
    public static final String UP_TOKEN = "[ .. ]";

    public String fileName;
    public String fileExt;
    public long fileLength;
    public String fileModifyDate;

    DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public FileModel() {
        this.fileName = UP_TOKEN;
        this.fileExt = "";
        this.fileLength = -2;
        this.fileModifyDate = "";
    }

    public FileModel(Path path) {
        try {
            if (!Files.isDirectory(path)) {
                if (path.getFileName().toString().startsWith(".")) {
                    this.fileName = path.getFileName().toString();
                    this.fileExt = "";
                } else {
                    this.fileName = path.getFileName().toString().split("\\.")[0];
                    this.fileExt = path.getFileName().toString().split("\\.")[1];
                }
                this.fileLength = Files.size(path);
            } else {
                this.fileName = path.getFileName().toString();
                this.fileExt = "[DIR]";
                this.fileLength = -1;
            }
            this.fileModifyDate = df.format(Files.getLastModifiedTime(path).toMillis());
        } catch (IOException e) {
            throw new RuntimeException("Something wrong with file: " +
                    path.toAbsolutePath());
        }
    }

    public boolean isDirectory() {
        return fileLength == -1L;
    }

    public boolean isUpElement() {
        return fileLength == -2L;
    }

    public String getFileName() {
        return fileName;
    }


    public String getFileExt() {
        return fileExt;
    }


    public long getFileLength() {
        return fileLength;
    }

    public String getFileModifyDate() {
        return fileModifyDate;
    }

    @Override
    public String toString() {
        return "ru.gb.file.manager.core.FileModel{" +
                "fileName='" + fileName + '\'' +
                ", fileExt='" + fileExt + '\'' +
                ", fileLength=" + fileLength +
                ", fileModifyDate='" + fileModifyDate +
                '}';
    }

    @Override
    public CommandTypes getType() {
        return CommandTypes.FILE_MODEL;
    }

}

