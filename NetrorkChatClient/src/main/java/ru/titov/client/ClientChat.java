package ru.titov.client;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.titov.client.controllers.AuthController;
import ru.titov.client.controllers.ClientController;
import ru.titov.client.model.Network;

import java.io.IOException;

public class ClientChat extends Application {

    public static ClientChat INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger(ClientChat.class);
    public static final String CONNECTION_ERROR_MESSAGE = "Невозможно установить сетевое соединение";

    private Stage primaryStage;
    private Stage authStage;
    private FXMLLoader chatWindowLoader;
    private FXMLLoader authLoader;

    @Override
    public void init() throws Exception {
        INSTANCE = this;
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        initViews();
        getChatStage().show();
        getAuthStage().show();
        getAuthController().initializeMessageHandler();

        this.primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                Network.getInstance().close();
            }
        });
    }

    private void initViews() throws IOException {
        initChatWindow();
        initAuthDialog();
    }

    private void initChatWindow() throws IOException {
        chatWindowLoader = new FXMLLoader();
        chatWindowLoader.setLocation(ClientChat.class.getResource("chat-template.fxml"));

        Parent root = chatWindowLoader.load();
        this.primaryStage.setScene(new Scene(root));
    }

    private void initAuthDialog() throws java.io.IOException {
        authLoader = new FXMLLoader();
        authLoader.setLocation(ClientChat.class.getResource("authDialog.fxml"));
        Parent authDialogPanel = authLoader.load();

        authStage = new Stage();
        authStage.initOwner(primaryStage);
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.setScene(new Scene(authDialogPanel));
    }

    public void switchToMainChatWindow(String username) {
        getChatStage().setTitle(username);
        getChatController().initializeMessageHandler();
        getAuthController().close();
        getAuthStage().close();
    }

    public void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Stage getAuthStage() {
        return authStage;
    }

    public static void main(String[] args) {
        launch();
    }

    private AuthController getAuthController() {
        return authLoader.getController();
    }

    private ClientController getChatController() {
        return chatWindowLoader.getController();
    }

    public Stage getChatStage() {
        return this.primaryStage;
    }
}