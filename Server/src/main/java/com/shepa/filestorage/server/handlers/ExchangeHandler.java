package com.shepa.filestorage.server.handlers;

import com.shepa.filestorage.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Обработчик сообщений от клиента
 */
public class ExchangeHandler extends ChannelInboundHandlerAdapter {

    private Path userRootDir;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // Получение типа входящего сообщения
        ExchangeMessageTypes msgType = ((ExchangeMessage) msg).getMessageType();

        if (msgType == ExchangeMessageTypes.UPDATE_FILES_LIST) {
            // Отправка информации о файлах в указанном каталоге
            UpdateFilesList updateFilesList = (UpdateFilesList) msg;

            /*
            В случае, если каталог не указан считаем,
            что нужно передать список файлов в корневом каталоге
             */
            Path directory = updateFilesList.getDirPath() == null ? userRootDir : userRootDir.resolve(updateFilesList.getDirPath());
            updateFilesList.fillFilesList(directory);
            ctx.writeAndFlush(updateFilesList);

        } else if (msgType == ExchangeMessageTypes.MOVE_FILE) {
            MoveMessage message = (MoveMessage) msg;
            if (message.getDestination() == MoveMessage.Destination.TO_SERVER) {
                // Отправка на сервер
                // Сформируем полный путь с учетом корневой директории
                Path newFilePath = userRootDir.resolve(message.getPath());
                if (message.isDirectory()) {
                    // Создание директории
                    if (!Files.exists(newFilePath)) {
                        Files.createDirectory(newFilePath);
                    }
                } else {
                    // Запись файла
                    Files.write(newFilePath, message.getData(), StandardOpenOption.CREATE);
                }
            } else if (message.getDestination() == MoveMessage.Destination.TO_CLIENT) {
                // Скачивание с сервера
                if (message.isDirectory()) {
                    // Если это директория, отправим ее на клиент со всем содержимым
                    FileSystemMethods.filesList(Paths.get(message.getPath()).toAbsolutePath())
                            .forEach(
                                    file -> {
                                        try {
                                            ctx.writeAndFlush(new MoveMessage(file, userRootDir.toAbsolutePath(), MoveMessage.Destination.TO_CLIENT));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                            );
                } else {
                    // Если это файл, отправим только его
                    ctx.writeAndFlush(new MoveMessage(Paths.get(message.getPath()).toAbsolutePath(), userRootDir.toAbsolutePath(), MoveMessage.Destination.TO_CLIENT));
                }
            }

        } else if (msgType == ExchangeMessageTypes.AUTH) {
            // Аутентикация
            AuthMessage authMsg = (AuthMessage) msg;

            // В случае успешного входа в систему проверим, существует ли на диске каталог пользователя
            // и при отсутствии создадим его
            if (authMsg.isLoginSuccessful()) {
                userRootDir = Paths.get("storage", authMsg.getUserDir());
                if (!Files.exists(userRootDir)) {
                    Files.createDirectory(userRootDir);
                }
            }

            // Отправим на клиент сообщение с результатом
            ctx.writeAndFlush(msg);

        }else if (msgType == ExchangeMessageTypes.RENAME_FILE) {
            // Переименование
            FileInfo file = ((RenameMessage) msg).getFile();
            String name = ((RenameMessage) msg).getName();

            if (file.isNew()) {
                // В случае, если это новый файл считаем, что была создана директория на сервере
                // Необходимо создать ее и присвоить имя
                try {
                    FileSystemMethods.createDirectory(userRootDir.resolve(file.getPathToFile() + File.separator + name).toString());
                    file.setFilename(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Файл/папка уже существует, переименуем
                try {
                    FileSystemMethods.rename(file, name);
                    file.setFilename(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (msgType == ExchangeMessageTypes.DELETE_FILE) {
            // Удаление с сервера
            FileInfo file = ((DeleteMessage) msg).getFile();

            if (file.isDirectory()) {
                try {
                    FileSystemMethods.removeFolder(Paths.get(file.getPathToFile()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Files.delete(Paths.get(file.getPathToFile()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            ctx.writeAndFlush(msg);
        }
    }

}
