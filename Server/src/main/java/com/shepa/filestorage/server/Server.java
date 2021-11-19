package com.shepa.filestorage.server;

import com.shepa.filestorage.server.handlers.AuthHandler;
import com.shepa.filestorage.server.handlers.ExchangeHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class Server {

    public static void main(String[] args) {
        System.out.println("Server started");
        try {

            // Создание директории для хранения пользовательских файлов
            Path storage = Paths.get("storage");
            if (!Files.exists(storage)) {
                Files.createDirectory(storage);
            }

            // Соединение с базой данных
            DBConnector.connect();

            // Запусе сервера
            new Server();

        } catch (ClassNotFoundException | IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public Server() {
        EventLoopGroup mainGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new ObjectDecoder(150*1024*1024, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler(),
                                    new ExchangeHandler(),
                                    new ChunkedWriteHandler()
                            );
                        }
                    });
            ChannelFuture future = serverBootstrap.bind(15005).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            DBConnector.disconnect();
        }
    }
}
