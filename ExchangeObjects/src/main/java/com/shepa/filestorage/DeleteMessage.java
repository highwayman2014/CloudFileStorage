package com.shepa.filestorage;

/**
 * Сообщение удаления
 */
public class DeleteMessage extends ExchangeMessage{

    private final FileInfo file;

    public DeleteMessage(FileInfo file) {
        super(ExchangeMessageTypes.DELETE_FILE);
        this.file = file;
    }

    public FileInfo getFile() {
        return file;
    }
}
