package ru.smirnov;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    public static final int PORT = 8080;
    private static AtomicInteger maxID = new AtomicInteger(1);
    private static Map<Integer, Session> sessionMap = new ConcurrentHashMap<>();

    private static Runnable processClient(Socket clientSocket) throws IOException {
        return () -> {
            try {
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                while (true) {
                    Message msg = (Message) in.readObject();
                    System.out.println(msg.from + ": " + msg.type + ": " + msg.data);
                    switch (msg.type) {
                        case MessageTypes.MT_INIT:
                            int id = maxID.getAndIncrement();
                            sessionMap.put(id, new Session());
                            Message sendMessage = new Message(-1, MessageTypes.MT_INIT, String.valueOf(id));
                            out.writeObject(sendMessage);
                            out.flush();
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
                            Message sendMsg = sessionMap.get(msg.from).getMessage();
                            out.writeObject(sendMsg);
                            out.flush();
                            break;
                    }
                }
            } catch (OptionalDataException e) {
                System.out.println("Exception length: " + e.length);
                System.out.println("Is EOF: " + e.eof);
                throw new RuntimeException(e);
            } catch (IOException | ClassNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.printf("Сервер запущен на порту %s\n", PORT);

            while (true) {
                new Thread(processClient(serverSocket.accept())).start();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
