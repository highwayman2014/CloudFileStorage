package com.shepa.filestorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сообщение передачи файла
 */
public class MoveMessage extends ExchangeMessage{

    public enum Destination {
        TO_CLIENT, // скачивание
        TO_SERVER // загрузка
    }

    private String path;
    private boolean isDirectory;
    private Destination destination;
    private byte[] data;

    public MoveMessage(Path path, Path rootDir, Destination destination) throws IOException {
        super(ExchangeMessageTypes.MOVE_FILE);
        this.path = rootDir.relativize(path).toString();
        this.destination = destination;
        if (Files.isDirectory(path)) {
            this.isDirectory = true;
        } else {
            this.isDirectory = false;
            this.data = Files.readAllBytes(path);
        }
    }

    public MoveMessage(FileInfo file, Destination destination) {
        super(ExchangeMessageTypes.MOVE_FILE);
        this.path = file.getPathToFile();
        this.destination = destination;
        this.isDirectory = file.isDirectory();
    }

    public Destination getDestination() {
        return destination;
    }

    public byte[] getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }
}
