package com.shepa.filestorage;

/**
 * Сообщение переименования
 */
public class RenameMessage extends ExchangeMessage{

    private FileInfo file;
    private String name;

    public RenameMessage(FileInfo file, String name) {
        super(ExchangeMessageTypes.RENAME_FILE);
        this.file = file;
        this.name = name;
    }

    public FileInfo getFile() {
        return file;
    }

    public String getName() {
        return name;
    }
}
