package com.shepa.filestorage;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Информация о файле
 * - имя файла,
 * - размер файла,
 * - дату последнего изменения,
 * - признак директории,
 * - путь к файлу
 * - признак нового файла
 */
public class FileInfo implements Serializable {

    private String filename;
    private long size;
    private LocalDateTime lastChange;
    private boolean isDirectory = false;
    private String pathToFile;
    private boolean isNew;

    public FileInfo(String folder, boolean isDirectory){
        this.isDirectory = isDirectory;
        this.lastChange = LocalDateTime.now();
        this.pathToFile = folder;
        this.isNew = true;
    }

    public FileInfo(Path path) {
        try {
            this.filename = path.getFileName().toString();
            if (Files.isDirectory(path)) {
                isDirectory = true;
                try {
                    this.size = FileSystemMethods.getDirectorySize(path);
                } catch (Exception e) {
                    this.size = 0L;
                }
            } else {
                this.size = Files.size(path);
            }
            this.pathToFile = path.toString();
            this.lastChange = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
            this.isNew = false;
        } catch (IOException e) {
            throw new RuntimeException("Problem with file");
        }
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getLastChange() {
        return lastChange;
    }

    public String getPathToFile() {
        return pathToFile;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        Path folder = Paths.get(this.pathToFile);
        if (!isNew) {
            folder = folder.getParent();
        }
        this.pathToFile = folder.resolve(filename).toString();
        isNew = false;
    }
}
