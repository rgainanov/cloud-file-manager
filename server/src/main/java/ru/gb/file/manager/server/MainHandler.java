package ru.gb.file.manager.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.gb.file.manager.core.FileModel;
import ru.gb.file.manager.core.ServerFileList;
import ru.gb.file.manager.core.TextMessage;
import ru.gb.file.manager.core.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MainHandler extends ChannelInboundHandlerAdapter {

    private AuthProvider authProvider;
    private Path serverRoot;
    private Path clientDir;

    public MainHandler() {
        this.authProvider = new DbAuthProvider();
        serverRoot = Paths.get("server", "SERVER_STORAGE");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("[ SERVER ]: Message received -> {}", msg.getClass().getName());
        if (msg instanceof User) {
            authProvider.start();
            User u = (User) msg;
            if (u.isNewUser()) {
                registerNewUser(u, ctx.channel());
            } else {
                authUser(u, ctx.channel());
            }
            authProvider.stop();
        }
    }

    @SneakyThrows
    private void authUser(User u, Channel c) {
        String authLogin = u.getLogin();
        String authPass = u.getPassword();
        String[] selectUser = authProvider.getUsers(u.getLogin());

        if (selectUser == null) {
            log.info("[ SERVER ]: Auth attempt, user -> {}. Error, user does not exists.", authLogin);
            c.writeAndFlush(new TextMessage("/auth_error_non_existing_user"));
            return;
        }
        if (!selectUser[1].equals(authPass)) {
            log.info("[ SERVER ]: Auth attempt, user -> {}. Error, incorrect password.", authLogin);
            c.writeAndFlush(new TextMessage("/auth_error_incorrect_pass"));
            return;
        }
        log.info("[ SERVER ]: Auth attempt, user -> {}. Success, user authenticated.", authLogin);
        clientDir = serverRoot.resolve(authLogin);
        if (!Files.exists(clientDir)) {
            Files.createDirectory(clientDir);
        }
        c.writeAndFlush(new TextMessage("/auth_ok"));
        c.writeAndFlush(new ServerFileList(scanFile(), clientDir.toString()));

    }

    @SneakyThrows
    private void registerNewUser(User u, Channel c) {
        String authLogin = u.getLogin();
        String authPass = u.getPassword();
        String[] selectUser = authProvider.getUsers(u.getLogin());

        if (selectUser != null) {
            log.info("[ SERVER ]: Sign attempt, user -> {}. Error, user exists.", authLogin);
            c.writeAndFlush(new TextMessage("/sign_user_exists"));
            return;
        }

        boolean isUserAdded =authProvider.addUserRecord(authLogin, authPass);
        if (!isUserAdded) {
            log.info("[ SERVER ]: Sign attempt, user -> {}. Error occurred.", authLogin);
            c.writeAndFlush("/sign_error_creating_user");
        } else {
            log.info("[ SERVER ]: Sign attempt, user -> {}. Success, user created.", authLogin);
            clientDir = serverRoot.resolve(authLogin);
            if (!Files.exists(clientDir)) {
                Files.createDirectory(clientDir);
            }
            c.writeAndFlush(new TextMessage("/auth_ok"));
            c.writeAndFlush(new ServerFileList(scanFile(), clientDir.toString()));

        }
    }

    public List<FileModel> scanFile() {
        try {
            List<FileModel> out = new ArrayList<>();
            out.add(new FileModel());
            List<Path> pathsInRoot = Files.list(clientDir).collect(Collectors.toList());
            for (Path p : pathsInRoot) {
                out.add(new FileModel(p));
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException("File scan exception: ");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("[ SERVER ]: Client connected -> {}", ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("[ SERVER ]: Client disconnected -> {}", ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.debug("[ SERVER ]: client dropped -> {}", cause.toString());
    }
}
