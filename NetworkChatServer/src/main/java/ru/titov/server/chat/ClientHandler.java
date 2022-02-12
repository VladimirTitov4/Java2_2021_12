package ru.titov.server.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.titov.clientserver.Command;
import ru.titov.clientserver.CommandType;
import ru.titov.clientserver.commands.AuthCommandData;
import ru.titov.clientserver.commands.PrivateMessageCommandData;
import ru.titov.clientserver.commands.PublicMessageCommandData;
import ru.titov.clientserver.commands.UpdateUsernameCommandData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler {

    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);

    private final MyServer server;
    private final Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String userName;

    public ClientHandler(MyServer server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        server.getExecutorService().execute(() -> {
            try {
                authenticate();
                readMessages();
            } catch (IOException e) {
                LOGGER.error("Failed to process message from client");
                e.printStackTrace();
            } finally {
                try {
                    closeConnection();
                } catch (IOException e) {
                    LOGGER.error("Failed to close connection");
                }
            }
        });
    }

    private void authenticate() throws IOException {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }

            if (command.getType() == CommandType.AUTH) {
                AuthCommandData data = (AuthCommandData) command.getData();
                String login = data.getLogin();
                String password = data.getPassword();
                String userName = server.getAuthService().getUserNameByLoginAndPassword(login, password);
                if (userName == null) {
                    sendCommand(Command.errorCommand("Некорректные логин и пароль"));
                } else if (server.isUsernameBusy(userName)) {
                    sendCommand(Command.errorCommand("Такой пользователь уже существует!"));
                } else {
                    this.userName = userName;
                    sendCommand(Command.authOkCommand(userName));
                    server.subscribe(this);
                    return;
                }
            }
        }
    }

    private Command readCommand() throws IOException {
        Command command = null;
        try {
            command = (Command) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to read Command class");
            e.printStackTrace();
        }

        return command;
    }

    private void closeConnection() throws IOException {
        server.unsubscribe(this);
        clientSocket.close();
    }

    private void readMessages() throws IOException {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }

            switch (command.getType()) {
                case END:
                    return;
                case PRIVATE_MESSAGE: {
                    PrivateMessageCommandData data = (PrivateMessageCommandData) command.getData();
                    String recipient = data.getReceiver();
                    String privateMessage = data.getMessage();
                    server.sendPrivateMessage(this, recipient, privateMessage);
                    break;
                }
                case PUBLIC_MESSAGE: {
                    PublicMessageCommandData data = (PublicMessageCommandData) command.getData();
                    processMessage(data.getMessage());
                    break;
                }
                case UPDATE_USERNAME: {
                    UpdateUsernameCommandData data = (UpdateUsernameCommandData) command.getData();
                    String newUsername = data.getUsername();
                    server.getAuthService().updateUsername(userName, newUsername);
                    userName = newUsername;
                    server.notifyClientUserListUpdated();
                    break;
                }
            }
        }
    }

    private void processMessage(String message) throws IOException {
        this.server.broadcastMessage(message, this);
    }

    public void sendCommand(Command command) throws IOException {
        outputStream.writeObject(command);
    }

    public String getUserName() {
        return userName;
    }
}
