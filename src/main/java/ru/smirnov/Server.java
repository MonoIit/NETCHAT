package ru.smirnov;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private final String serverName = "server";
    private final Logger logger;
    private final int PORT;

    public Server(int port) {
        this.PORT = port;
        logger = Logger.getInstance();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.logAndPrint(serverName, "Сервер запущен на порту " + PORT);

            while (true) {
                new Thread(processClient(serverSocket.accept())).start();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    Runnable processClient(Socket clientSocket) throws IOException {
        return () -> {
            try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
                while (true) {
                    Message msg = (Message) ois.readObject();
                    logger.log(serverName, (msg.from + ": " + msg.type + ": " + msg.data));
                    switch (msg.type) {
                        case MessageTypes.MT_INIT:
                            if (msg.from.isEmpty() || msg.from.equals(serverName) || sessionMap.containsKey(msg.from)) {
                                Message.sendMsg(oos, serverName, MessageTypes.MT_ERROR, "Это имя уже занято");
                            } else {
                                sessionMap.put(msg.from, new Session());
                                Message.sendMsg(oos, serverName, MessageTypes.MT_CONFIRM, "");
                            }
                            break;
                        case MessageTypes.MT_EXIT:
                            sessionMap.remove(msg.from);
                            clientSocket.close();
                            return;
                        case MessageTypes.MT_DATA:
                            for (Session session : sessionMap.values()) {
                                session.pushMessage(new Message(msg.from, MessageTypes.MT_DATA, msg.data));
                            }
                            break;
                        case MessageTypes.MT_GETDATA:
                            sessionMap.get(msg.from).getMessage().send(oos);
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            } finally {
                try {
                    clientSocket.close();
                    logger.log(serverName, "сервер разорвал подключение");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        };
    }

    public static void main(String[] args) throws IOException {
        SettingsLoader settings = new SettingsLoader("settings.properties");
        int port = settings.getPort();
        new Server(port).start();
    }

    public Map<String, Session> getSessionMap() {
        return sessionMap;
    }
}
