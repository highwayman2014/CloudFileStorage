package com.shepa.filestorage.server.handlers;

import com.shepa.filestorage.*;
import com.shepa.filestorage.server.DBConnector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Обработчик запросов авторизации/регистрации от клиента
 * Для остальных соощений пропускается
 */
public class AuthHandler extends SimpleChannelInboundHandler<AuthMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AuthMessage msg) throws Exception {

        if (msg.getMessageType() == ExchangeMessageTypes.AUTH) {
            // Авторизация
            String userDir = DBConnector.checkAuth(msg.getLogin(), msg.getPassword());
            msg.setLoginSuccessful(userDir);
            ctx.fireChannelRead(msg);
        } else if (msg.getMessageType() == ExchangeMessageTypes.REGISTRATION) {
            // Регистрация
            DBConnector.addUser(msg.getLogin(), msg.getPassword());
            ctx.writeAndFlush(msg);
        }
    }

}
