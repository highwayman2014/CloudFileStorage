package com.shepa.filestorage;

/**
 * Типы сообщений обмена
 */
public enum ExchangeMessageTypes {
    AUTH,               // Аутентикация
    REGISTRATION,       // Регистрация
    UPDATE_FILES_LIST,  // Обновление списка файлов
    MOVE_FILE,          // Скачивание/загрузка
    RENAME_FILE,        // Переименование
    DELETE_FILE         // Удаление
}