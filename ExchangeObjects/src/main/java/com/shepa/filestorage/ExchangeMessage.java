package com.shepa.filestorage;

import java.io.Serializable;

/**
 * Суперкласс для сообщений обмена
 * Содержит тип сообщения
 */
public class ExchangeMessage implements Serializable {

    private final ExchangeMessageTypes messageType;

    public ExchangeMessage(ExchangeMessageTypes messageType) {
        this.messageType = messageType;
    }

    public ExchangeMessageTypes getMessageType() {
        return messageType;
    }

}
