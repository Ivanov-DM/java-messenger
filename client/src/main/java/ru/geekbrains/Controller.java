package ru.geekbrains;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.PlatformLoggingMXBean;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean authorized;
    private String nick;
    private ObservableList<String> clientsList;

    @FXML
    TextField msgField, login;

    @FXML
    PasswordField password;

    @FXML
    TextArea mainTextArea;

    @FXML
    HBox msgPanel;

    @FXML
    VBox authPanel;

    @FXML
    ListView<String> clientsView;

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        if (this.authorized) {
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            clientsView.setVisible(true);
            clientsView.setManaged(true);
        } else {
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            nick = " ";
            clientsView.setVisible(false);
            clientsView.setManaged(false);
        }
    }

    public void connect() {
        try {
            setAuthorized(false);
            socket = new Socket("localhost", 8081);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            String str =in.readUTF();
                            if (str.startsWith("/authok")) {
                                nick = str.split(" ")[1];
                                setAuthorized(true);
                                sendCustomMsg("/history");
                                break;
                            }
//                            mainTextArea.appendText(str);
//                            mainTextArea.appendText("\n");
                            showAlert(str);
                        }

                        while (true) {
                            String str =in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.startsWith("/clientslist")) {
                                    String[] tokens = str.split(" ");
                                    Platform.runLater(() -> {
                                        clientsList.clear();
                                        for (int i = 1; i < tokens.length ; i++) {
                                            clientsList.add(tokens[i]);
                                        }
                                    });
                                }
                                continue;
                            }
                            mainTextArea.appendText(str);
                            mainTextArea.appendText("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        setAuthorized(false);
                        showAlert("You are disconnect");
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCustomMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + login.getText() + " " + password.getText());
            login.clear();
            password.clear();
        } catch (IOException e) {
            e.printStackTrace();
            login.clear();
            password.clear();
            showAlert("Невозможно отправить сообщение, проверьте сетевое соединение");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        clientsList = FXCollections.observableArrayList();
        clientsView.setItems(clientsList);

//        class MyListCell extends ListCell<String> {
//            @Override
//            protected void updateItem(String item, boolean empty) {
//                super.updateItem(item, empty);
//            }
//        }
//        clientsView.setCellFactory(param -> new MyListCell());
    }

    public void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            alert.showAndWait();
        });
    }

    public void clickClientList(MouseEvent mouseEvent) {
        if(mouseEvent.getClickCount() == 2) {
            String str = clientsView.getSelectionModel().getSelectedItem();
            msgField.setText("/w " + str + " ");
            msgField.requestFocus();
            msgField.selectEnd();
        }
    }
}
