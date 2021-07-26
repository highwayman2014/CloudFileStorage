package com.shepa.filestorage.client;

import com.shepa.filestorage.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ClientStageController implements Initializable {

    @FXML
    VBox signInPane;
    @FXML
    TextField loginField;
    @FXML
    TextField passwordField;
    @FXML
    Label warningMsg;
    @FXML
    Label lblWelcome;
    @FXML
    Button btnSignIn;
    @FXML
    Button btnSignUp;
    @FXML
    HBox lblBottomSingIn;
    @FXML
    HBox lblBottomSingUp;

    @FXML
    VBox mainPane;
    @FXML
    TextField clientSearch;
    @FXML
    TextField serverSearch;

    @FXML
    TableView<FileInfo> clientFilesList;
    @FXML
    TableView<FileInfo> serverFilesList;
    @FXML
    TextField clientPath;
    @FXML
    TextField serverPath;

    private ServerHandler serverHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Запуск потока-обработчика входящих и исходящих сообщений с сервером
        try {
            serverHandler = new ServerHandler(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Создание таблиц файлов и описание колонок
        describeTable(clientFilesList);
        describeTable(serverFilesList);

        //Слушатели для поиска файлов
        clientSearch.textProperty().addListener(observable -> {
            String filter = clientSearch.getText();
            if (filter == null || filter.length() == 0) {
                updateClientFilesList(Paths.get(clientPath.getText()));
            } else {
                try {
                    clientFilesList.getItems().clear();
                    clientFilesList.getItems().addAll(FileSystemMethods.searchInCurrentDir(filter, clientPath.getText()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        serverSearch.textProperty().addListener(observable -> {
            String filter = serverSearch.getText();
            if (filter == null || filter.length() == 0) {
                serverHandler.sendMessage(new UpdateFilesList(serverPath.getText()));
            } else {
                serverHandler.sendMessage(new UpdateFilesList(serverPath.getText(), filter));
            }
        });

    }

    /**
     * Получение списка файлов на клиенте и заполнение пути
     * @param path
     */
    public void updateClientFilesList(Path path) {
        try {
            clientPath.setText(path.normalize().toAbsolutePath().toString());
            clientFilesList.getItems().clear();
            clientFilesList.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            clientFilesList.sort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void describeTable(TableView table) {

        table.setEditable(true);

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setPrefWidth(250);
        fileNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        fileNameColumn.setOnEditCommit(this::editFileName);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(70);
        fileSizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formatBytesCount(item));
                }
            }
        });

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateChangeColumn = new TableColumn<>("Last change");
        fileDateChangeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastChange().format(dateTimeFormatter)));
        fileDateChangeColumn.setPrefWidth(180);

        table.getColumns().addAll(fileNameColumn, fileSizeColumn, fileDateChangeColumn);

    }

    /**
     * Обработчик события переименования файла
     * @param event
     */
    private void editFileName(TableColumn.CellEditEvent<FileInfo, String> event) {
        String newName = event.getNewValue();
        if (!"".equals(newName)) {
            TablePosition<FileInfo, String> position = event.getTablePosition();
            int row = position.getRow();
            FileInfo file = event.getTableView().getItems().get(row);
            if (event.getTableView().equals(clientFilesList)) {
                if (file.isNew()) {
                    try {
                        FileSystemMethods.createDirectory(clientPath.getText() + File.separator + newName);
                        file.setFilename(newName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        FileSystemMethods.rename(file, newName);
                        file.setFilename(newName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (event.getTableView().equals(serverFilesList)) {
                serverHandler.sendMessage(new RenameMessage(file, newName));
                serverHandler.sendMessage(new UpdateFilesList(serverPath.getText()));
            }
        }

    }

    /**
     * Метод преобразует размер файла типа Long
     * в читаемую строку с приставкой (B - байты, KB - килобайты...)
     * @param bytes
     * @return
     */
    private String formatBytesCount(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Остановка потока-обработчика соединения с сервером и закрытие клиентского окна
     * @param actionEvent
     */
    public void exitAction(ActionEvent actionEvent) {
        serverHandler.shutdown();
        Platform.exit();
    }

    /**
     * Обработчик клика по клиентской таблице
     * @param mouseEvent
     */
    public void clientTableClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            Path path = Paths.get(clientPath.getText()).resolve(clientFilesList.getSelectionModel().getSelectedItem().getFilename());
            if (Files.isDirectory(path)) {
                updateClientFilesList(path);
            }
        }
    }

    /**
     * Обработчик клика по серверной таблице
     * @param mouseEvent
     */
    public void serverTableClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            FileInfo currentFile = serverFilesList.getSelectionModel().getSelectedItem();
            if (currentFile.isDirectory()) {
                serverHandler.sendMessage(
                        new UpdateFilesList(
                                (serverPath.getText() == null ? "" : serverPath.getText() + File.separator) + currentFile.getFilename()));
            }
        }
    }

    /**
     * Переход на уровень выше по пути клиента
     * @param actionEvent
     */
    public void setClientPathUp(ActionEvent actionEvent) {
        Path parent = Paths.get(clientPath.getText()).getParent();
        if (parent != null) {
            updateClientFilesList(parent);
        }
    }

    /**
     * Переход на уровень выше по пути сервера
     * @param actionEvent
     */
    public void setServerPathUp(ActionEvent actionEvent) {
        Path parent = Paths.get(serverPath.getText()).getParent();
        if (parent != null) {
            serverHandler.sendMessage(new UpdateFilesList(parent.toString()));
        } else {
            serverHandler.sendMessage(new UpdateFilesList());
        }
    }

    /**
     * Запуск загрузки на сервер
     */
    public void copyToServer() {
        FileInfo currentFile = clientFilesList.getSelectionModel().getSelectedItem();
        if (currentFile != null) {
            try {
                if (currentFile.isDirectory()) {
                    // Если это директория, отправим на сервер каждый файл, сожержащийся в ней
                    FileSystemMethods.filesList(Paths.get(currentFile.getPathToFile()))
                            .forEach(
                                file -> {
                                    try {
                                        serverHandler.sendMessage(new MoveMessage(file.toAbsolutePath(), Paths.get(clientPath.getText()), MoveMessage.Destination.TO_SERVER));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                    );
                } else {
                    serverHandler.sendMessage(
                            new MoveMessage(Paths.get(currentFile.getPathToFile()).toAbsolutePath(), Paths.get(clientPath.getText()), MoveMessage.Destination.TO_SERVER));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverHandler.sendMessage(new UpdateFilesList(serverPath.getText()));
        }
    }

    /**
     * Запуск скачивания с сервера
     */
    public void copyFromServer() {
        FileInfo currentFile = serverFilesList.getSelectionModel().getSelectedItem();
        if (currentFile != null) {
            serverHandler.sendMessage(new MoveMessage(currentFile, MoveMessage.Destination.TO_CLIENT));
        }
    }

    public void signIn(ActionEvent actionEvent) {
        warningMsg.setText("Invalid login or password");
        serverHandler.sendMessage(new AuthMessage(loginField.getText(), passwordField.getText(), ExchangeMessageTypes.AUTH));
    }

    public void signUp(ActionEvent actionEvent) {
        String login = loginField.getText();
        if ("".equals(login)) {
            warningMsg.setVisible(true);
            warningMsg.setText("Login cannot be empty");
            return;
        }
        serverHandler.sendMessage(new AuthMessage(login, passwordField.getText(), ExchangeMessageTypes.REGISTRATION));
        goToSingIn(new ActionEvent());
    }

    public void setAuthorized(boolean isAuthorized) {
        if (isAuthorized) {
            signInPane.setVisible(false);
            mainPane.setVisible(true);
            Path defaultClientPath = Paths.get(".");
            updateClientFilesList(defaultClientPath);
            serverHandler.sendMessage(new UpdateFilesList());
        } else {
            warningMsg.setVisible(true);
        }
    }

    public void goToSingUp(ActionEvent actionEvent) {
        lblWelcome.setText("Create Account");
        btnSignIn.setVisible(false);
        lblBottomSingIn.setVisible(false);
        btnSignUp.setVisible(true);
        lblBottomSingUp.setVisible(true);
        warningMsg.setVisible(false);
    }

    public void goToSingIn(ActionEvent actionEvent) {
        lblWelcome.setText("Welcome!");
        btnSignIn.setVisible(true);
        lblBottomSingIn.setVisible(true);
        btnSignUp.setVisible(false);
        lblBottomSingUp.setVisible(false);
        warningMsg.setVisible(false);
    }

    //Обработчики событий контекстного меню

    public void addDirOnClient(ActionEvent actionEvent) {
        clientFilesList.getItems().add(new FileInfo(clientPath.getText(), true));
        int tablePosition = clientFilesList.getItems().size()-1;
        TableColumn<FileInfo, ?> fileInfoTableColumn = clientFilesList.getColumns().get(0);
        clientFilesList.edit(tablePosition, fileInfoTableColumn);
    }

    public void removeOnClient(ActionEvent actionEvent) {
        FileInfo currentFile = clientFilesList.getSelectionModel().getSelectedItem();
        if (currentFile.isDirectory()) {
            try {
                FileSystemMethods.removeFolder(Paths.get(currentFile.getPathToFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Files.delete(Paths.get(currentFile.getPathToFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateClientFilesList(Paths.get(clientPath.getText()));
    }

    public void renameOnClient(ActionEvent actionEvent) {
        int tablePosition = clientFilesList.getSelectionModel().getSelectedIndex();
        TableColumn<FileInfo, ?> fileInfoTableColumn = clientFilesList.getColumns().get(0);
        clientFilesList.edit(tablePosition, fileInfoTableColumn);
    }

    public void addDirOnServer(ActionEvent actionEvent) {
        serverFilesList.getItems().add(new FileInfo(serverPath.getText(), true));
        int tablePosition = serverFilesList.getItems().size()-1;
        TableColumn<FileInfo, ?> fileInfoTableColumn = serverFilesList.getColumns().get(0);
        serverFilesList.edit(tablePosition, fileInfoTableColumn);
    }

    public void renameOnServer(ActionEvent actionEvent) {
        int tablePosition = serverFilesList.getSelectionModel().getSelectedIndex();
        TableColumn<FileInfo, ?> fileInfoTableColumn = serverFilesList.getColumns().get(0);
        serverFilesList.edit(tablePosition, fileInfoTableColumn);
    }

    public void removeOnServer(ActionEvent actionEvent) {
        FileInfo currentFile = serverFilesList.getSelectionModel().getSelectedItem();
        serverHandler.sendMessage(new DeleteMessage(currentFile));
        serverHandler.sendMessage(new UpdateFilesList(serverPath.getText()));
    }
}