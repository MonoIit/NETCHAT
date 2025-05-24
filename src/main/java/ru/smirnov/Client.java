package ru.smirnov;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private String clientName = "";
    private volatile boolean isRunning = false;
    private final int PORT;
    private final Logger logger;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Thread receiver;


    public Client(int port) {
        this.PORT = port;
        this.logger = Logger.getInstance();
    }

    public void start() throws InterruptedException {
        try {
            connect();
            chooseName();
            receiver = startReceiving();
            handleUserInput();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            shutdown();
        }
    }

    private void connect() throws IOException {
        socket = new Socket("127.0.0.1", PORT);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    private void chooseName() throws IOException {
        Scanner sc = new Scanner(System.in);
        while (!isRunning) {
            logger.logAndPrint("newClient", ("Введите имя:"));
            String name = sc.nextLine().trim();
            if (name.isEmpty()) continue;
            logger.log("newClient", name);
            init(name);
        }
    }

    private void init(String name) throws IOException {
        Message.sendMsg(oos, name, MessageTypes.MT_INIT, "");
        try {
            Message receiveMessage = (Message) ois.readObject();
            if (receiveMessage.type == MessageTypes.MT_ERROR) {
                logger.logAndPrint("newClient", "Это имя уже занято");
                return;
            }
            clientName = name;
            isRunning = true;
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    private Thread startReceiving() {
        Thread proccesServer = new Thread(() -> {
            try {
                while (isRunning) {
                    Message.sendMsg(oos, clientName, MessageTypes.MT_GETDATA, "");
                    Message receiveMessage = (Message) ois.readObject();
                    switch (receiveMessage.type) {
                        case MT_DATA:
                            System.out.println(receiveMessage.from + ": " + receiveMessage.data);
                            logger.log(clientName, (receiveMessage.from + ": " + receiveMessage.data));
                            break;
                        default:
                            Thread.sleep(500);
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        proccesServer.start();
        return proccesServer;
    }

    private void handleUserInput() throws IOException {
        Scanner sc = new Scanner(System.in);
        while (isRunning) {
            String data = sc.nextLine();
            if (data.isEmpty()) continue;
            logger.log(clientName, data);
            if (data.equals("/exit")) {
                Message.sendMsg(oos, clientName, MessageTypes.MT_EXIT, "");
                isRunning = false;
                break;
            }
            else {
                Message.sendMsg(oos, clientName, MessageTypes.MT_DATA, data);
            }
            //Thread.sleep(500);
        }
    }

    private void shutdown() throws InterruptedException {
        isRunning = false;
        if (receiver != null && receiver.isAlive()) {
            receiver.join();
        }
        try {
            if (oos != null) oos.close();
            if (ois != null) ois.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SettingsLoader settings = new SettingsLoader("settings.properties");
        int port = settings.getPort();
        new Client(port).start();
    }

    public String getClientName() {
        return clientName;
    }

    public boolean getIsRunning() {
        return isRunning;
    }
}
