package ru.gb.file.manager.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.gb.file.manager.core.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

@Slf4j
public class ClientController implements Initializable {

    private static final int BUFFER_SIZE = 8 * 1024;

    private static final String HOST = "localhost";
    private static final int PORT = 8189;

    private byte[] buf;

    //    Client View Side
    public TextField clientPathField;
    public TableView<FileModel> clientTableView;
    public TableColumn<FileModel, String> clientColumnFileName;
    public TableColumn<FileModel, String> clientColumnFileExt;
    public TableColumn<FileModel, Long> clientColumnFileLength;
    public TableColumn<FileModel, String> clientColumnFileModifyDate;
    public Button clientDeleteButton;

    // Server View Side
    public String serverCurrentDir;
    public TextField serverPathField;
    public TableView<FileModel> serverTableView;
    public TableColumn<FileModel, String> serverColumnFileName;
    public TableColumn<FileModel, String> serverColumnFileExt;
    public TableColumn<FileModel, Long> serverColumnFileLength;
    public TableColumn<FileModel, String> serverColumnFileModifyDate;
    public Button serverCreateFileButton;
    public Button serverCreateDirectoryButton;
    public Button serverDeleteButton;
    public Button serverRenameButton;
    public Button serverShareFileButton;

    public Text infoMessageTextField;

    // Sign/login
    public TextField loginField;
    public PasswordField passwordField;
    public Button signInButton;
    public Button logInButton;

    public Button downloadButtonMiddle;
    public Button downloadButtonBottom;
    public Button uploadButtonMiddle;
    public Button uploadButtonBottom;

    public ProgressBar progressBar;

    private Path clientRoot;
    private Path clientDefaultParent;
    private ObservableList<FileModel> clientFilesObservableList;
    private ObservableList<FileModel> serverFilesObservableList;

    private Socket socket;
    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    @SneakyThrows
    public void menuItemFileExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buf = new byte[BUFFER_SIZE];
        clientDefaultParent = Paths.get("client");
        clientRoot = Paths.get("client", "CLIENT_STORAGE");
        initializeTableViews();
        clientNavigateToPath(clientRoot);
        try {
            socket = new Socket(HOST, PORT);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        Message msg = (Message) is.readObject();
                        log.debug("[ CLIENT ]: Message Received -> {}", msg);

                        switch (msg.getType()) {
                            case SERVER_RESPONSE_TXT_MESSAGE:
                                serverTextMessageHandler((ServerResponseTextMessage) msg);
                                break;
                            case SERVER_RESPONSE_FILE_LIST:
                                serverFileListMessageHandler((ServerResponseFileList) msg);
                                break;
                            case FILE_TRANSFER:
                                serverFileReceiver((FileTransfer) msg);
                                break;
                            case SERVER_RESPONSE_REGISTERED_USERS:
                                registeredUsers((ServerResponseRegisteredUsers) msg);
                                break;
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
            log.debug("[ CLIENT ]: Started ...");
        } catch (Exception e) {
            log.error("", e);

        }
    }

    @SneakyThrows
    private void registeredUsers(ServerResponseRegisteredUsers msg) {
        FutureTask<String> futureTask = new FutureTask<>(
                new ShareDialogPrompt(msg.getRegisteredUsers())
        );
        Platform.runLater(futureTask);
        String user = futureTask.get();
        FileModel fm = serverTableView.getSelectionModel().getSelectedItem();
        String shareFileName = getFileNameWithExtension(fm);
        Path shareFilePath = Paths.get(serverCurrentDir).resolve(shareFileName);
        log.debug("[ CLIENT ]: User to share with -> {}, file name -> {}, path -> {}",
                user, shareFileName, shareFilePath);
        os.writeObject(new ClientRequestFileShare(user, shareFilePath.toString()));
        os.flush();
    }

    static class ShareDialogPrompt implements Callable<String> {
        private final List<String> registeredUsers;

        ShareDialogPrompt(List<String> registeredUsers) {
            this.registeredUsers = registeredUsers;
        }

        @Override
        public String call() throws Exception {
            ChoiceDialog dialog = new ChoiceDialog(registeredUsers.get(0), registeredUsers);
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/stylesheet.css").toExternalForm());
            dialogPane.getStyleClass().add("dialog");
            dialog.setTitle("Cloud File Manager");
            dialog.setHeaderText("File share dialog");
            dialog.setContentText("Select user to share file with");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                return result.get();
            }
            return null;
        }
    }

    private void serverTextMessageHandler(ServerResponseTextMessage textMessage) {
        String tm = textMessage.getMsg();

        if (tm.equals("/auth_error_non_existing_user")) {
            infoMessageTextField.setText("Error occurred. No such user. Please Sign In.");
            signInButton.setManaged(true);
            signInButton.setVisible(true);
        }
        if (tm.equals("/auth_error_incorrect_pass")) {
            infoMessageTextField.setText("Error occurred. Login or Password is incorrect.");
        }
        if (tm.equals("/sign_user_exists")) {
            infoMessageTextField.setText("Error occurred. User with this login already exists.");
        }
        if (tm.equals("/sign_error_creating_user")) {
            infoMessageTextField.setText("Error occurred. Please contact support.");
        }
        if (tm.equals("/auth_ok")) {
            infoMessageTextField.setText("Successfully authenticated.");
            passwordField.clear();
            passwordField.setDisable(true);
//            loginField.clear();
            loginField.setDisable(true);
//            progressBar.setVisible(true);
//            progressBar.setManaged(true);
            logInButton.setDisable(true);
            signInButton.setDisable(true);
            downloadButtonMiddle.setDisable(false);
            downloadButtonBottom.setDisable(false);
            uploadButtonMiddle.setDisable(false);
            uploadButtonBottom.setDisable(false);
            serverCreateFileButton.setDisable(false);
            serverCreateDirectoryButton.setDisable(false);
            serverDeleteButton.setDisable(false);
            serverRenameButton.setDisable(false);
            serverShareFileButton.setDisable(false);
        }
        if (tm.equals("/file_create_error")) {
            infoMessageTextField.setText("Error occurred. Cannot create file.");
        }
        if (tm.equals("/directory_create_error")) {
            infoMessageTextField.setText("Error occurred. Cannot create directory.");
        }
        if (tm.equals("/file_uploaded")) {
            infoMessageTextField.setText("File Uploaded Successfully.");
        }
        if (tm.equals("/path_deleted")) {
            infoMessageTextField.setText("Success");
        }
        if (tm.equals("/directory_renamed")) {
            infoMessageTextField.setText("Success. Remote directory renamed");
        }
        if (tm.equals("/file_renamed")) {
            infoMessageTextField.setText("Success. Remote file renamed");
        }
        if (tm.equals("/file_shared")) {
            infoMessageTextField.setText("Success. File shared");
        }
    }

    private boolean receiveFile(FileTransfer msg, Path filePath) {
        log.debug("[ CLIENT ]: Receiving file -> {}", msg.getFileName());
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            while (true) {
                fos.write(msg.getFilePart(), 0, msg.getBatchLength());
                infoMessageTextField.setText("Receiving file " + msg.getFileName());

                double progress = (1.0 * msg.getCurrentBatch()) / msg.getBatchCount();

//                Platform.runLater(() -> {
//                    progressBar.setProgress(progress);
//                });

                log.debug(
                        "[ CLIENT ]: Receiving file -> {}, {}/{} - {}",
                        msg.getFileName(),
                        msg.getCurrentBatch(),
                        msg.getBatchCount(),
                        progress
                );
                if (msg.getBatchCount() == msg.getCurrentBatch()) {
                    break;
                }
                msg = (FileTransfer) is.readObject();
            }
//            progressBar.setProgress(0.0);
//            progressBar.setDisable(true);
            return true;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    @SneakyThrows
    private void serverFileReceiver(FileTransfer msg) {
        FileModel fileModel = clientTableView.getSelectionModel().getSelectedItem();
        if (fileModel == null) {
            Path filePath = clientRoot.resolve(msg.getFileName());
            boolean isFileReceived = receiveFile(msg, filePath);
            if (isFileReceived) {
                clientNavigateToPath(clientRoot);
                infoMessageTextField.setText("File Downloaded Successfully.");
                return;
            } else {
                infoMessageTextField.setText("Error occurred. File has not been downloaded");
            }
        }
        if (fileModel != null && fileModel.isDirectory()) {
            Path filePath = clientRoot.resolve(fileModel.getFileName()).resolve(msg.getFileName());
            boolean isFileReceived = receiveFile(msg, filePath);
            if (isFileReceived) {
                clientNavigateToPath(clientRoot);
                infoMessageTextField.setText("File Downloaded Successfully.");
            } else {
                infoMessageTextField.setText("Error occurred. File has not been downloaded");
            }
        } else {
            infoMessageTextField.setText("Error occurred. Cannot download file into file.");
        }
    }

    private void serverFileListMessageHandler(ServerResponseFileList msg) {
        serverFilesObservableList = FXCollections.observableArrayList(msg.getList());
        serverCurrentDir = msg.getServerPath();
        serverPathField.setText(serverCurrentDir);
        serverTableView.getItems().clear();
        serverTableView.setItems(serverFilesObservableList);
        infoMessageTextField.setText("Server file list updated");
    }

    public void menuItemDisconnectFromServer(ActionEvent actionEvent) {
    }

    @SneakyThrows
    public void signInButtonAction(ActionEvent actionEvent) {
        ClientRequestAuthUser user = new ClientRequestAuthUser(loginField.getText(), passwordField.getText(), true);
        os.writeObject(user);
        os.flush();

    }

    @SneakyThrows
    public void logInButtonAction(ActionEvent actionEvent) {
        ClientRequestAuthUser user = new ClientRequestAuthUser(loginField.getText(), passwordField.getText());
        os.writeObject(user);
        os.flush();
    }

    private void tableViewContextMenu(TableView<FileModel> tableView) {
        tableView.setRowFactory(new Callback<TableView<FileModel>, TableRow<FileModel>>() {
            @Override
            public TableRow<FileModel> call(TableView<FileModel> param) {
                final TableRow<FileModel> tableRow = new TableRow<>();
                final ContextMenu tableItemContextMenu = new ContextMenu();
                MenuItem renameItem = new MenuItem("Rename");
                MenuItem deleteItem = new MenuItem("Delete");
                MenuItem transferItem = new MenuItem("Transfer");
                MenuItem shareItem = new MenuItem("Share");
                renameItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        log.debug("[ CLIENT ]: Rename action");
                        renameAction(tableView);
                    }
                });

                deleteItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        log.debug("[ CLIENT ]: Delete action");
                        deleteAction(tableView);
                    }
                });

                transferItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        log.debug("[ CLIENT ]: Transfer action");
                        transferAction(tableView);
                    }
                });

                shareItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        log.debug("[ CLIENT ]: Share action");
                    }
                });

                tableItemContextMenu.getItems().addAll(renameItem, deleteItem, transferItem, shareItem);
                tableRow.contextMenuProperty().bind(
                        Bindings.when(tableRow.emptyProperty())
                                .then((ContextMenu) null)
                                .otherwise(tableItemContextMenu)
                );
                return tableRow;
            }
        });
    }

    @SneakyThrows
    private void transferAction(TableView<FileModel> tableView) {
        FileModel clientSelectedFileModel = clientTableView.getSelectionModel().getSelectedItem();
        FileModel serverSelectedFileModel = serverTableView.getSelectionModel().getSelectedItem();
        if (tableView.equals(clientTableView)) {
            if (clientSelectedFileModel != null) {

                if (!clientSelectedFileModel.isDirectory()) {
                    String fileName = getFileNameWithExtension(clientSelectedFileModel);
                    Path filePath = clientRoot.resolve(fileName);
                    Path destinationFilePath = Paths.get(serverCurrentDir);
                    if (serverSelectedFileModel != null) {
                        if (serverSelectedFileModel.isDirectory()) {
                            destinationFilePath = destinationFilePath.resolve(serverSelectedFileModel.getFileName()).resolve(fileName);
                        } else {
                            infoMessageTextField.setText("Error occurred. Cannot upload into file.");
                        }
                    } else {
                        destinationFilePath = destinationFilePath.resolve(fileName);
                    }
                    infoMessageTextField.setText("Uploading file : " + fileName);
                    sendFile(filePath, destinationFilePath);
                } else {
                    infoMessageTextField.setText("Error occurred. Cannot upload directory.");
                }
            } else {
                infoMessageTextField.setText("Error occurred. Select directory or file for upload.");
            }
        } else {
            if (serverSelectedFileModel != null) {
                if (!serverSelectedFileModel.isDirectory()) {
                    Path p = Paths.get(
                            serverCurrentDir,
                            (serverSelectedFileModel.getFileName() + "." + serverSelectedFileModel.getFileExt())
                    );
                    os.writeObject(new ClientRequestFile(p));
                    os.flush();
                } else {
                    infoMessageTextField.setText("Error occurred. Cannot download directory.");
                }
            } else {
                infoMessageTextField.setText("Error occurred. Please select file to download.");
            }
        }
    }

    private void initializeTableViews() {
        tableViewContextMenu(clientTableView);
        prepColumns(clientColumnFileName, clientColumnFileExt, clientColumnFileLength, clientColumnFileModifyDate);

        tableViewContextMenu(serverTableView);
        prepColumns(serverColumnFileName, serverColumnFileExt, serverColumnFileLength, serverColumnFileModifyDate);
    }

    private void prepColumns(
            TableColumn<FileModel, String> columnFileName,
            TableColumn<FileModel, String> columnFileExt,
            TableColumn<FileModel, Long> columnFileLength,
            TableColumn<FileModel, String> columnFileModifyDate) {

        columnFileName.setCellValueFactory(new PropertyValueFactory<>("FileName"));
        columnFileExt.setCellValueFactory(new PropertyValueFactory<>("FileExt"));
        columnFileLength.setCellValueFactory(new PropertyValueFactory<>("FileLength"));
        columnFileModifyDate.setCellValueFactory(new PropertyValueFactory<>("FileModifyDate"));

        columnFileLength.setCellFactory(new Callback<TableColumn<FileModel, Long>, TableCell<FileModel, Long>>() {
            @Override
            public TableCell<FileModel, Long> call(TableColumn<FileModel, Long> param) {
                return new TableCell<FileModel, Long>() {
                    @Override
                    protected void updateItem(Long item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText("");
                        } else {
                            if (item == -1L || item == -2L) {
                                setText("");
                            } else {
                                setText(item + " B");
                            }
                        }
                    }
                };
            }
        });
    }

    public void clientNavigateToPath(Path path) {
        clientRoot = path;
        clientFilesObservableList = FXCollections.observableArrayList(scanFiles(path));
        clientPathField.setText(clientRoot.toString());
        clientTableView.getItems().clear();
        clientTableView.setItems(clientFilesObservableList);
    }

    public List<FileModel> scanFiles(Path path) {
        try {
            List<FileModel> out = new ArrayList<>();
            out.add(new FileModel());
            List<Path> pathsInRoot = Files.list(path).collect(Collectors.toList());
            for (Path p : pathsInRoot) {
                out.add(new FileModel(p));
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException("File scan exception: " + clientRoot);
        }
    }

    public void clientTableViewClickedItem(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            FileModel fileModel = clientTableView.getSelectionModel().getSelectedItem();
            if (fileModel != null) {
                if (fileModel.isDirectory()) {
                    Path pathTo = clientRoot.resolve(fileModel.getFileName());
                    clientNavigateToPath(pathTo);
                }
                if (fileModel.isUpElement()) {
                    Path pathTo = clientRoot.getParent();
                    if (!pathTo.equals(clientDefaultParent)) {
                        clientNavigateToPath(pathTo);
                    }
                }
            }
        }
    }

    @SneakyThrows
    public void serverTableViewClickedItem(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            FileModel fileModel = serverTableView.getSelectionModel().getSelectedItem();
            if (fileModel != null) {
                if (fileModel.isDirectory()) {
                    os.writeObject(new ClientRequestGoIn(serverCurrentDir, fileModel.getFileName()));
                }
                if (fileModel.isUpElement()) {
                    os.writeObject(new ClientRequestGoUp(serverCurrentDir));
                }
                os.flush();
            }
        }
    }

    @SneakyThrows
    public void serverFileCreateButtonAction(ActionEvent actionEvent) {
        String newRemoteFileName = genericPopUpDialog("Create", "file");
        if (newRemoteFileName != null) {
            FileModel serverSelectedFileModel = serverTableView.getSelectionModel().getSelectedItem();
            if (serverSelectedFileModel != null) {
                Path p = Paths.get(serverCurrentDir, serverSelectedFileModel.getFileName());
                os.writeObject(new ClientRequestFileCreate(newRemoteFileName, p.toString()));
            } else {
                os.writeObject(new ClientRequestFileCreate(newRemoteFileName, serverCurrentDir));
            }
            os.flush();
            log.debug("[ CLIENT ]: Request for file creation sent to Server, file name -> {}.", newRemoteFileName);
        }
    }

    @SneakyThrows
    public void serverDirectoryCreateButtonAction(ActionEvent actionEvent) {
        String newRemoteDirectoryName = genericPopUpDialog("Create", "directory");
        if (newRemoteDirectoryName != null) {
            FileModel serverSelectedFileModel = serverTableView.getSelectionModel().getSelectedItem();
            if (serverSelectedFileModel != null) {
                if (serverSelectedFileModel.isDirectory()) {
                    Path p = Paths.get(serverCurrentDir, serverSelectedFileModel.getFileName(), newRemoteDirectoryName);
                    os.writeObject(new ClientRequestDirectoryCreate(p.toString()));
                } else {
                    infoMessageTextField.setText("Error occurred. Please select directory.");
                }
            } else {
                Path p = Paths.get(serverCurrentDir, newRemoteDirectoryName);
                os.writeObject(new ClientRequestDirectoryCreate(p.toString()));
            }
            os.flush();
            log.debug("[ CLIENT ]: Request for directory creation sent to Server, directory name -> {}.", newRemoteDirectoryName);
        }
    }

    @SneakyThrows
    public void serverDownloadButtonAction(ActionEvent actionEvent) {
        transferAction(serverTableView);
    }

    @SneakyThrows
    private void sendFile(Path localFilePath, Path destinationFilePath) {
        File file = localFilePath.toFile();
        long fileLength = file.length();
        long batchCount = (fileLength + BUFFER_SIZE - 1) / BUFFER_SIZE;
        int currentBatch = 1;

        try (FileInputStream fis = new FileInputStream(file)) {
            while (fis.available() > 0) {
                int read = fis.read(buf);
                os.writeObject(new FileTransfer(
                        localFilePath.getFileName().toString(),
                        destinationFilePath.toString(),
                        batchCount,
                        currentBatch,
                        read,
                        buf
                ));

                double progress = (1.0 * currentBatch) / batchCount;

//                progressBar.setProgress(progress);

                log.debug(
                        "[ CLIENT ]: Uploading file -> {}, {}/{} - {}",
                        localFilePath.getFileName().toString(),
                        currentBatch,
                        batchCount,
                        progress
                );

                currentBatch++;
            }
        }
        os.flush();
        log.info("[ Client ]: File -> {}, transfer finished.", localFilePath.getFileName());
    }


    @SneakyThrows
    public void serverUploadButtonAction(ActionEvent actionEvent) {
        transferAction(clientTableView);
    }

    private String getFileNameWithExtension(FileModel fm) {
        StringBuilder sb = new StringBuilder();
        if (!fm.isDirectory()) {
            sb.append(fm.getFileName());
            sb.append(".");
            sb.append(fm.getFileExt());
        } else {
            sb.append(fm.getFileName());
        }
        return sb.toString();
    }

    @SneakyThrows
    private void deleteAction(TableView<FileModel> tableView) {
        FileModel fm = tableView.getSelectionModel().getSelectedItem();
        if (tableView.equals(clientTableView)) {
            Path filePath = clientRoot.resolve(getFileNameWithExtension(fm));
            log.debug("[ CLIENT ]: To be deleted -> {}", filePath);

            if (fm != null) {
                if (Files.isDirectory(filePath)) {
                    Files.walk(filePath)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } else {
                    Files.delete(filePath);
                }
                clientNavigateToPath(filePath.getParent());
                return;
            }
            infoMessageTextField.setText("Error occurred. Select directory or file to be deleted.");
        } else {
            Path remoteToBeDeletedPath;

            if (fm != null) {
                remoteToBeDeletedPath = Paths.get(serverCurrentDir).resolve(getFileNameWithExtension(fm));
                os.writeObject(new ClientRequestDelete(remoteToBeDeletedPath.toString()));
                os.flush();
                log.debug("[ CLIENT ]: Requesting Remote to delete -> {}", remoteToBeDeletedPath);
            } else {
                infoMessageTextField.setText("Error occurred. Select Remote directory or file to be deleted.");
            }
        }
    }

    @SneakyThrows
    public void clientDeleteButtonAction(ActionEvent actionEvent) {
        deleteAction(clientTableView);
    }

    @SneakyThrows
    public void serverDeleteButtonAction(ActionEvent actionEvent) {
        deleteAction(serverTableView);
    }

    private String genericPopUpDialog(String action, String type) {
        TextInputDialog dialog = new TextInputDialog();
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/stylesheet.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog");
        dialog.setTitle("Cloud File Manager");
        dialog.setHeaderText(action + " " + type + " dialog");
        dialog.setContentText("Enter " + type + " name : ");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return result.get().trim();
        }
        return null;
    }


    @SneakyThrows
    public void clientCreateFileButtonAction(ActionEvent actionEvent) {
        // ToDo Replace strings with Paths
        String newFileName = genericPopUpDialog("Create", "file");
        if (newFileName != null) {
            FileModel clientSelectedFileModel = clientTableView.getSelectionModel().getSelectedItem();
            if (clientSelectedFileModel != null && clientSelectedFileModel.isDirectory()) {
                Path p = clientRoot.resolve(clientSelectedFileModel.getFileName()).resolve(newFileName);
                Files.createFile(p);
                infoMessageTextField.setText("Success. File Created.");
                clientNavigateToPath(p.getParent());
            } else {
                Path p = clientRoot.resolve(newFileName);
                Files.createFile(p);
                infoMessageTextField.setText("Success. File Created.");
                clientNavigateToPath(p.getParent());
            }

            log.debug("[ CLIENT ]: File created, directory name -> {}.", newFileName);
        }
    }

    @SneakyThrows
    public void clientCreateDirectoryButtonAction(ActionEvent actionEvent) {
        String newDirectoryName = genericPopUpDialog("Create", "directory");
        if (newDirectoryName != null) {
            FileModel clientSelectedFileModel = clientTableView.getSelectionModel().getSelectedItem();
            if (clientSelectedFileModel != null && clientSelectedFileModel.isDirectory()) {
                Path p = clientRoot.resolve(clientSelectedFileModel.getFileName()).resolve(newDirectoryName);
                Files.createDirectory(p);
                infoMessageTextField.setText("Success. Directory Created.");
                clientNavigateToPath(p);
            } else {
                Path p = clientRoot.resolve(newDirectoryName);
                Files.createDirectory(p);
                infoMessageTextField.setText("Success. Directory Created.");
                clientNavigateToPath(p);
            }

            log.debug("[ CLIENT ]: Directory created, directory name -> {}.", newDirectoryName);
        }
    }

    @SneakyThrows
    private void renameAction(TableView<FileModel> tableView) {
        FileModel fm = tableView.getSelectionModel().getSelectedItem();
        if (tableView.equals(clientTableView)) {
            if (fm != null) {
                if (!fm.isDirectory()) {
                    String newName = genericPopUpDialog("Rename", "file");
                    if (newName != null) {
                        String currentName = getFileNameWithExtension(fm);
                        Path currentPath = clientRoot.resolve(currentName);
                        Path newPath = clientRoot.resolve(newName);
                        log.debug("[ CLIENT ]: File rename request -> {}, new file name -> {}",
                                currentName, newName
                        );

                        Files.move(currentPath, newPath);
                        clientNavigateToPath(newPath.getParent());
                        infoMessageTextField.setText("Success. File renamed.");
                    } else {
                        infoMessageTextField.setText("Error occurred. Enter new file name");
                    }

                } else {
                    String newName = genericPopUpDialog("Rename", "directory");
                    if (newName != null) {
                        String currentName = getFileNameWithExtension(fm);
                        Path currentPath = clientRoot.resolve(currentName);
                        Path newPath = clientRoot.resolve(newName);
                        log.debug("[ CLIENT ]: Directory rename request -> {}, new file name -> {}",
                                currentName, newName
                        );

                        Files.move(currentPath, currentPath.resolveSibling(newName));
                        clientNavigateToPath(newPath.getParent());
                        infoMessageTextField.setText("Success. Directory renamed.");
                    } else {
                        infoMessageTextField.setText("Error occurred. Enter new directory name");
                    }
                }
            } else {
                infoMessageTextField.setText("Error occurred. Select file or directory to be renamed.");
            }
        } else {
            if (fm != null) {
                String newName = genericPopUpDialog("Rename", "file");
                if (newName != null) {
                    String currentName = getFileNameWithExtension(fm);
                    String currentPath = Paths.get(serverCurrentDir).resolve(currentName).toString();
                    String newPath = Paths.get(serverCurrentDir).resolve(newName).toString();
                    os.writeObject(new ClientRequestRename(currentPath, newPath));
                    os.flush();
                    log.debug("[ CLIENT ]: Remote rename request sent. Old name -> {}, new name -> {}",
                            currentName, newName
                    );
                }
            } else {
                infoMessageTextField.setText("Error occurred. Select file or directory to be renamed.");
            }
        }
    }

    @SneakyThrows
    public void clientRenameButtonAction(ActionEvent actionEvent) {
        renameAction(clientTableView);
    }

    @SneakyThrows
    public void serverRenameButtonAction(ActionEvent actionEvent) {
        renameAction(serverTableView);
    }

    @SneakyThrows
    public void serverShareFileButtonAction(ActionEvent actionEvent) {
        log.debug("[ CLIENT ]: Requesting list of users.");
        FileModel fm = serverTableView.getSelectionModel().getSelectedItem();
        if (fm != null) {
            if (!fm.isDirectory()) {
                os.writeObject(new ClientRequestAllUsers());
                os.flush();
                return;
            }
        }
        infoMessageTextField.setText("Error occurred. Select file to be shared.");
    }
}
