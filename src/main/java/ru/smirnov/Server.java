package ru.smirnov;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static String serverName = "server";
    private static Logger logger;
    private static int PORT;

    private static Runnable processClient(Socket clientSocket) throws IOException {
        return () -> {
            try {
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                while (true) {
                    Message msg = (Message) in.readObject();
                    logger.log(serverName, (msg.from + ": " + msg.type + ": " + msg.data));
                    switch (msg.type) {
                        case MessageTypes.MT_INIT:
                            if (msg.from.isEmpty() || msg.from.equals(serverName) || sessionMap.containsKey(msg.from)) {
                                Message.sendMsg(out, serverName, MessageTypes.MT_ERROR, "Это имя уже занято");
                                break;
                            }
                            sessionMap.put(msg.from, new Session());
                            Message.sendMsg(out, serverName, MessageTypes.MT_CONFIRM, "");
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
                            sessionMap.get(msg.from).getMessage().send(out);
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
        PORT = settings.getPort();
        logger = Logger.getInstance();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.logAndPrint(serverName, "Сервер запущен на порту " + PORT);

            while (true) {
                new Thread(processClient(serverSocket.accept())).start();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
