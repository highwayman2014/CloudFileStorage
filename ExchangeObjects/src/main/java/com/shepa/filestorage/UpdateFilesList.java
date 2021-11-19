package com.shepa.filestorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сообщение обновления списка файлов
 */
public class UpdateFilesList extends ExchangeMessage{

    private String dirPath;
    private String filter;
    private List<FileInfo> filesList = new ArrayList<>();

    public UpdateFilesList() {
        super(ExchangeMessageTypes.UPDATE_FILES_LIST);
    }

    public UpdateFilesList(String dirPath) {
        super(ExchangeMessageTypes.UPDATE_FILES_LIST);
        if (!"".equals(dirPath)) {
            this.dirPath = dirPath;
        }
    }

    public UpdateFilesList(String dirPath, String filter) {
        super(ExchangeMessageTypes.UPDATE_FILES_LIST);
        this.dirPath = dirPath;
        this.filter = filter;
    }

    public String getDirPath() {
        return dirPath;
    }

    public List<FileInfo> getFilesList() {
        return filesList;
    }

    public void fillFilesList(Path path) throws IOException {
        if (filter == null) {
            filesList = Files.list(path).map(FileInfo::new).collect(Collectors.toList());
        } else {
            // В случае заполненного фильтра сформируем список найденных файлов
            filesList = FileSystemMethods.searchInCurrentDir(filter, path.toString());
        }

    }
}
