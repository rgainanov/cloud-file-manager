package ru.gb.file.manager.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
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

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
    public TextField serverPathField;
    public TableView<FileModel> serverTableView;
    public TableColumn<FileModel, String> serverColumnFileName;
    public TableColumn<FileModel, String> serverColumnFileExt;
    public TableColumn<FileModel, Long> serverColumnFileLength;
    public TableColumn<FileModel, String> serverColumnFileModifyDate;

    public Text infoMessageTextField;

    // Sign/login
    public TextField loginField;
    public TextField passwordField;
    public Button signInButton;
    public Button logInButton;

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
        goToPath(clientRoot);
        try {
            Socket socket = new Socket(HOST, PORT);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        Message msg = (Message) is.readObject();
                        log.debug("[ CLIENT ]: Message Received -> {}", msg);

                        if (msg instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) msg;
                            String tm = textMessage.getMsg();

                            if (tm.equals("/auth_error_non_existing_user")) {
                                infoMessageTextField.setText("Error occurred. No such user. Please Sign In.");
                                signInButton.setManaged(true);
                                signInButton.setVisible(true);
                                continue;
                            }
                            if (tm.equals("/auth_error_incorrect_pass")) {
                                infoMessageTextField.setText("Error occurred. Login or Password is incorrect.");
                                continue;
                            }
                            if (tm.equals("/sign_user_exists")) {
                                infoMessageTextField.setText("Error occurred. User with this login already exists.");
                                continue;
                            }
                            if (tm.equals("/sign_error_creating_user")) {
                                infoMessageTextField.setText("Error occurred. Please contact support.");
                            }
                        } else if (msg instanceof ServerFileList) {
                            ServerFileList serverFileList = (ServerFileList) msg;
                            serverFilesObservableList = FXCollections.observableArrayList(serverFileList.getList());
                            serverPathField.setText(serverFileList.getServerPath());
                            serverTableView.getItems().clear();
                            serverTableView.setItems(serverFilesObservableList);
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

    public void menuItemDisconnectFromServer(ActionEvent actionEvent) {

    }

    @SneakyThrows
    public void signInButtonAction(ActionEvent actionEvent) {
        User user = new User(loginField.getText(), passwordField.getText(), true);
        os.writeObject(user);
        os.flush();

    }

    @SneakyThrows
    public void logInButtonAction(ActionEvent actionEvent) {
        User user = new User(loginField.getText(), passwordField.getText());
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

    public void goToPath(Path path) {
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
                    goToPath(pathTo);
                }
                if (fileModel.isUpElement()) {
                    Path pathTo = clientRoot.getParent();
                    if (!pathTo.equals(clientDefaultParent)) {
                        goToPath(pathTo);
                    }
                }
            }
        }
    }

    public void serverTableViewClickedItem(MouseEvent mouseEvent) {

    }
}
