package com.shepa.filestorage.client;

import com.shepa.filestorage.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Класс-обработчик обмена данными с сервером
 */
public class ServerHandler {

    private ExecutorService service;

    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;

    private ClientStageController clientStage;

    public ServerHandler(ClientStageController clientStageController) throws IOException {
        startConnection();
        service = Executors.newSingleThreadExecutor();
        service.execute(serverListener);
        clientStage = clientStageController;
    }

    /**
     * Запуск соединения с сервером и инициализация входящего и искходящего потоков
     * @throws IOException
     */
    public void startConnection() throws IOException {
        socket = new Socket("localhost", 15005);
        in = new ObjectDecoderInputStream(socket.getInputStream(), 20971520);
        out = new ObjectEncoderOutputStream(socket.getOutputStream());
    }

    /**
     * Отправка сообщения на сервер
     * @param msg
     * @param <T>
     */
    public <T extends ExchangeMessage> void sendMessage (T msg) {
        try {
            if (msg.getMessageType() == ExchangeMessageTypes.UPDATE_FILES_LIST) {
                clientStage.serverFilesList.getItems().clear();
            }
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    public void shutdown() {
        try {
            socket.close();
            service.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Runnable serverListener = () -> {
        try {
            while (true) {
                Object msg = readObject();
                if (((ExchangeMessage) msg).getMessageType() == ExchangeMessageTypes.UPDATE_FILES_LIST) {
                    // Получение списка файлов в каталоге на сервере
                    UpdateFilesList updateFilesList = (UpdateFilesList) msg;

                    // Заполненим текущий путь на и таблицу файлов на форме
                    clientStage.serverPath.setText(updateFilesList.getDirPath());
                    clientStage.serverFilesList.getItems().addAll(updateFilesList.getFilesList());

                } else if (((ExchangeMessage) msg).getMessageType() == ExchangeMessageTypes.AUTH) {
                    // Получение и обработка результата входа в систему
                    clientStage.setAuthorized(((AuthMessage) msg).isLoginSuccessful());

                } else if (((ExchangeMessage) msg).getMessageType() == ExchangeMessageTypes.MOVE_FILE) {
                    // Получение файла с сервера
                    MoveMessage moveMsg = (MoveMessage) msg;
                    Path newFilePath = Paths.get(clientStage.clientPath.getText(), moveMsg.getPath());
                    if (moveMsg.isDirectory()) {
                        if (!Files.exists(newFilePath)) {
                            Files.createDirectory(newFilePath);
                        }
                    } else {
                        Files.write(newFilePath, moveMsg.getData(), StandardOpenOption.CREATE);
                    }

                    // Обновление списка файлов на клиенте
                    clientStage.updateClientFilesList(Paths.get(clientStage.clientPath.getText()));

                }
            }
        } catch (SocketException e) {
            System.out.println("exit");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    };

}
