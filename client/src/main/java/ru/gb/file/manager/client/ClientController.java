package ru.gb.file.manager.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.gb.file.manager.core.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class ClientController implements Initializable {

    private static final String HOST = "localhost";
    private static final int PORT = 8189;

    //    Client View Side
    public TextField clientPathField;
    public TableView<FileModel> clientTableView;
    public TableColumn<FileModel, String> clientColumnFileName;
    public TableColumn<FileModel, String> clientColumnFileExt;
    public TableColumn<FileModel, Long> clientColumnFileLength;
    public TableColumn<FileModel, String> clientColumnFileModifyDate;

    // Server View Side
    public String serverCurrentDir;
    public TextField serverPathField;
    public TableView<FileModel> serverTableView;
    public TableColumn<FileModel, String> serverColumnFileName;
    public TableColumn<FileModel, String> serverColumnFileExt;
    public TableColumn<FileModel, Long> serverColumnFileLength;
    public TableColumn<FileModel, String> serverColumnFileModifyDate;

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
    public Button serverCreateFileButton;
    public Button serverCreateDirectoryButton;
    public Button serverRemoveButton;
    public ProgressBar progressBar;

    private Path clientRoot;
    private Path clientDefaultParent;
    private ObservableList<FileModel> clientFilesObservableList;
    private ObservableList<FileModel> serverFilesObservableList;

    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    public void menuItemFileExitAction(ActionEvent actionEvent) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clientDefaultParent = Paths.get("client");
        clientRoot = Paths.get("client", "CLIENT_STORAGE");
        initializeTableViews();
        clientNavigateToPath(clientRoot);
        try {
            Socket socket = new Socket(HOST, PORT);
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
                            case SERVER_RESPONSE_FILE:
                                serverFileReceiver((ServerResponseFile) msg);
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
            logInButton.setDisable(true);
            signInButton.setDisable(true);
            downloadButtonMiddle.setDisable(false);
            downloadButtonBottom.setDisable(false);
            uploadButtonMiddle.setDisable(false);
            uploadButtonBottom.setDisable(false);
            serverCreateFileButton.setDisable(false);
            serverCreateDirectoryButton.setDisable(false);
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
    }

    private boolean receiveFile(ServerResponseFile msg, Path filePath) {
        log.debug("[ CLIENT ]: Receiving file -> {}", msg.getFileName());
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            while (true) {
                fos.write(msg.getFilePart(), 0, msg.getBatchLength());
                infoMessageTextField.setText("Receiving file " + msg.getFileName());

                double progress = (1.0 * msg.getCurrentBatch()) / msg.getBatchCount();

                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                });

//                infoMessageTextField.setText("Receiving file " + msg.getFileName() +
//                        " part " + msg.getCurrentBatch() + "/" + msg.getBatchCount() + ".");
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
                msg = (ServerResponseFile) is.readObject();
            }
            return true;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    @SneakyThrows
    private void serverFileReceiver(ServerResponseFile msg) {
        FileModel fileModel = clientTableView.getSelectionModel().getSelectedItem();
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        if (fileModel == null) {
            Path filePath = clientRoot.resolve(msg.getFileName());
            boolean isFileReceived = receiveFile(msg, filePath);
            if (isFileReceived) {
                clientNavigateToPath(clientRoot);
                infoMessageTextField.setText("File Downloaded Successfully.");
            } else {
                infoMessageTextField.setText("Error occurred. File has not been downloaded");
            }
//            Files.write(filePath, msg.getFilePart());
//            clientNavigateToPath(clientRoot);
//            infoMessageTextField.setText("File Downloaded Successfully.");
        }
        if (fileModel != null && fileModel.isDirectory()) {
            Path filePath = clientRoot.resolve(fileModel.getFileName()).resolve(msg.getFileName());
//            Files.write(filePath, msg.getFilePart());
//            clientNavigateToPath(filePath.getParent());
//            infoMessageTextField.setText("File Downloaded Successfully.");
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

    private void initializeTableViews() {
        clientColumnFileName.setCellValueFactory(new PropertyValueFactory<>("FileName"));
        clientColumnFileExt.setCellValueFactory(new PropertyValueFactory<>("FileExt"));
        clientColumnFileLength.setCellValueFactory(new PropertyValueFactory<>("FileLength"));
        clientColumnFileModifyDate.setCellValueFactory(new PropertyValueFactory<>("FileModifyDate"));

        clientColumnFileLength.setCellFactory(new Callback<TableColumn<FileModel, Long>, TableCell<FileModel, Long>>() {
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

        serverColumnFileName.setCellValueFactory(new PropertyValueFactory<>("FileName"));
        serverColumnFileExt.setCellValueFactory(new PropertyValueFactory<>("FileExt"));
        serverColumnFileLength.setCellValueFactory(new PropertyValueFactory<>("FileLength"));
        serverColumnFileModifyDate.setCellValueFactory(new PropertyValueFactory<>("FileModifyDate"));

        serverColumnFileLength.setCellFactory(new Callback<TableColumn<FileModel, Long>, TableCell<FileModel, Long>>() {
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
        // ToDo Replace strings with Paths
        TextInputDialog dialog = new TextInputDialog();
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/stylesheet.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog");
        dialog.setTitle("Cloud File Manager");
        dialog.setHeaderText("Create File dialog");
        dialog.setContentText("Enter File name : ");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            FileModel serverSelectedFileModel = serverTableView.getSelectionModel().getSelectedItem();
            if (serverSelectedFileModel != null) {
                Path p = Paths.get(serverCurrentDir, serverSelectedFileModel.getFileName());
                os.writeObject(new ClientRequestFileCreate(result.get().trim(), p.toString()));
            } else {
                os.writeObject(new ClientRequestFileCreate(result.get().trim(), serverCurrentDir));
            }
            os.flush();
            log.debug("[ CLIENT ]: Request for file creation sent to Server, file name -> {}.", result.get());
        }
    }

    @SneakyThrows
    public void serverDirectoryCreateButtonAction(ActionEvent actionEvent) {
        // ToDo Replace strings with Paths
        TextInputDialog dialog = new TextInputDialog();
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/stylesheet.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog");
        dialog.setTitle("Cloud File Manager");
        dialog.setHeaderText("Create Directory dialog");
        dialog.setContentText("Enter Directory name : ");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            FileModel serverSelectedFileModel = serverTableView.getSelectionModel().getSelectedItem();
            if (serverSelectedFileModel != null) {
                if (serverSelectedFileModel.isDirectory()) {
                    Path p = Paths.get(serverCurrentDir, serverSelectedFileModel.getFileName(), result.get().trim());
                    os.writeObject(new ClientRequestDirectoryCreate(p.toString()));
                } else {
                    infoMessageTextField.setText("Error occurred. Please select directory.");
                }
            } else {
                Path p = Paths.get(serverCurrentDir, result.get().trim());
                os.writeObject(new ClientRequestDirectoryCreate(p.toString()));
            }
            os.flush();
            log.debug("[ CLIENT ]: Request for directory creation sent to Server, directory name -> {}.", result.get());
        }
    }

    @SneakyThrows
    public void serverDownloadButtonAction(ActionEvent actionEvent) {
        FileModel serverSelectedFileModel = serverTableView.getSelectionModel().getSelectedItem();
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

    @SneakyThrows
    public void serverUploadButtonAction(ActionEvent actionEvent) {
        FileModel clientSelectedFileModel = clientTableView.getSelectionModel().getSelectedItem();
        FileModel serverSelectedFileModel = serverTableView.getSelectionModel().getSelectedItem();
        if (clientSelectedFileModel != null) {
            if (!clientSelectedFileModel.isDirectory()) {
                String fileName = clientSelectedFileModel.getFileName() + "." + clientSelectedFileModel.getFileExt();
                Path filePath = clientRoot.resolve(fileName);
                Path newServerFileDirectory = Paths.get(serverCurrentDir);
                if (serverSelectedFileModel != null) {
                    if (serverSelectedFileModel.isDirectory()) {
                        newServerFileDirectory = newServerFileDirectory.resolve(serverSelectedFileModel.getFileName()).resolve(fileName);
                    } else {
                        infoMessageTextField.setText("Error occurred. Cannot upload into file.");
                    }
                } else {
                    newServerFileDirectory = newServerFileDirectory.resolve(fileName);
                }
                byte[] file = Files.readAllBytes(filePath);
                os.writeObject(new ClientFileTransfer(newServerFileDirectory.toString(), file));
                os.flush();
            } else {
                infoMessageTextField.setText("Error occurred. Cannot upload directory.");
            }
        }
    }
}
