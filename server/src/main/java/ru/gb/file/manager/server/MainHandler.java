package ru.gb.file.manager.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.gb.file.manager.core.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MainHandler extends SimpleChannelInboundHandler<Message> {

    private static final int BUFFER_SIZE = 8 * 1024;

    private AuthProvider authProvider;
    private Path serverRoot;
    private Path clientDir;

    private byte[] buf;

    public MainHandler(AuthProvider authProvider) {
        this.authProvider = authProvider;
        this.buf = new byte[BUFFER_SIZE];
        this.serverRoot = Paths.get("server", "SERVER_STORAGE");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        log.debug("[ SERVER ]: Message received -> {}", msg.getClass().getName());

        switch (msg.getType()) {
            case AUTH_REQUEST:
                ClientRequestAuthUser u = (ClientRequestAuthUser) msg;
                if (u.isNewUser()) {
                    registerNewUser(u, ctx.channel());
                } else {
                    authUser(u, ctx.channel());
                }
                break;
            case CLIENT_FILE_REQUEST:
                sendFileToClient((ClientRequestFile) msg, ctx.channel());
                break;
            case CLIENT_REQUEST_FILE_DELETE:
                break;
            case CLIENT_REQUEST_MKDIR:
                createNewDirectory((ClientRequestDirectoryCreate) msg, ctx.channel());
                break;
            case CLIENT_REQUEST_TOUCH:
                createNewFile((ClientRequestFileCreate) msg, ctx.channel());
                break;
            case CLIENT_REQUEST_PATH_GO_IN:
                goToPath((ClientRequestGoIn) msg, ctx.channel());
                break;
            case CLIENT_REQUEST_PATH_GO_UP:
                goToPathUp((ClientRequestGoUp) msg, ctx.channel());
                break;
            case CLIENT_FILE_TRANSFER:
                receiveFileFromClient((ClientFileTransfer) msg, ctx.channel());
                break;
        }
    }

    @SneakyThrows
    private void receiveFileFromClient(ClientFileTransfer m, Channel c) {
        Path filePath = Paths.get(m.getFilePath());
        Files.write(filePath, m.getFile());
        c.writeAndFlush(new ServerResponseFileList(scanFile(filePath.getParent()), filePath.getParent().toString()));
        c.writeAndFlush(new ServerResponseTextMessage("/file_uploaded"));
    }

    @SneakyThrows
    private void sendFileToClient(ClientRequestFile m, Channel c) {
        Path filePath = Paths.get(m.getFilePath());
        String fileName = filePath.getFileName().toString();
        File file = filePath.toFile();

        long fileLength = file.length();
        long batchCount = (fileLength + BUFFER_SIZE - 1) / BUFFER_SIZE;
        int currentBatch = 1;

        try (FileInputStream fis = new FileInputStream(file)) {
            while (fis.available() > 0) {
                int read = fis.read(buf);
                c.writeAndFlush(new ServerResponseFile(fileName, batchCount, currentBatch, read, buf));
                currentBatch++;
                log.info("[ SERVER ]: File -> {}, part {}/{} sent.", fileName, currentBatch, batchCount);
            }
        }
//        c.flush();
        log.info("[ SERVER ]: File -> {}, transfer finished.", fileName);

    }

    @SneakyThrows
    private void createNewDirectory(ClientRequestDirectoryCreate msg, Channel c) {
        Path dirPath = Paths.get(msg.getNewDir());
        if (!Files.exists(dirPath)) {
            Files.createDirectory(dirPath);
            c.writeAndFlush(new ServerResponseFileList(scanFile(dirPath), dirPath.toString()));
        } else {
            c.writeAndFlush(new ServerResponseTextMessage("/directory_create_error"));
        }
    }

    private void goToPathUp(ClientRequestGoUp clientRequestGoUp, Channel c) {
        Path serverCurrentDir = Paths.get(clientRequestGoUp.getCurrentDir());
        if (!serverCurrentDir.equals(clientDir)) {
            Path newPath = serverCurrentDir.getParent();
            c.writeAndFlush(new ServerResponseFileList(scanFile(newPath), newPath.toString()));
        }
    }

    private void goToPath(ClientRequestGoIn clientRequestGoIn, Channel c) {
        Path newPath = Paths.get(clientRequestGoIn.getCurrentDir(), clientRequestGoIn.getFileName());
        c.writeAndFlush(new ServerResponseFileList(scanFile(newPath), newPath.toString()));
    }

    @SneakyThrows
    private void createNewFile(ClientRequestFileCreate msg, Channel c) {
        Path filePath = Paths.get(msg.getFilePath(), msg.getFileName());
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
            c.writeAndFlush(new ServerResponseFileList(scanFile(filePath.getParent()), filePath.getParent().toString()));
        } else {
            c.writeAndFlush(new ServerResponseTextMessage("/file_create_error"));
        }
    }

    @SneakyThrows
    private void authUser(ClientRequestAuthUser u, Channel c) {
        String authLogin = u.getLogin();
        String authPass = u.getPassword();
        String[] selectUser = authProvider.getUsers(u.getLogin());

        if (selectUser == null) {
            log.info("[ SERVER ]: Auth attempt, user -> {}. Error, user does not exists.", authLogin);
            c.writeAndFlush(new ServerResponseTextMessage("/auth_error_non_existing_user"));
            return;
        }
        if (!selectUser[1].equals(authPass)) {
            log.info("[ SERVER ]: Auth attempt, user -> {}. Error, incorrect password.", authLogin);
            c.writeAndFlush(new ServerResponseTextMessage("/auth_error_incorrect_pass"));
            return;
        }
        log.info("[ SERVER ]: Auth attempt, user -> {}. Success, user authenticated.", authLogin);
        clientDir = serverRoot.resolve(authLogin);
        if (!Files.exists(clientDir)) {
            Files.createDirectory(clientDir);
        }
        c.writeAndFlush(new ServerResponseTextMessage("/auth_ok"));
        c.writeAndFlush(new ServerResponseFileList(scanFile(clientDir), clientDir.toString()));

    }

    @SneakyThrows
    private void registerNewUser(ClientRequestAuthUser u, Channel c) {
        String authLogin = u.getLogin();
        String authPass = u.getPassword();
        String[] selectUser = authProvider.getUsers(u.getLogin());

        if (selectUser != null) {
            log.info("[ SERVER ]: Sign attempt, user -> {}. Error, user exists.", authLogin);
            c.writeAndFlush(new ServerResponseTextMessage("/sign_user_exists"));
            return;
        }

        boolean isUserAdded = authProvider.addUserRecord(authLogin, authPass);
        if (!isUserAdded) {
            log.info("[ SERVER ]: Sign attempt, user -> {}. Error occurred.", authLogin);
            c.writeAndFlush("/sign_error_creating_user");
        } else {
            log.info("[ SERVER ]: Sign attempt, user -> {}. Success, user created.", authLogin);
            clientDir = serverRoot.resolve(authLogin);
            if (!Files.exists(clientDir)) {
                Files.createDirectory(clientDir);
            }
            c.writeAndFlush(new ServerResponseTextMessage("/auth_ok"));
            c.writeAndFlush(new ServerResponseFileList(scanFile(clientDir), clientDir.toString()));

        }
    }

    public List<FileModel> scanFile(Path path) {
        try {
            List<FileModel> out = new ArrayList<>();
            out.add(new FileModel());
            List<Path> pathsInRoot = Files.list(path).collect(Collectors.toList());
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
