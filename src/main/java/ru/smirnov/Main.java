package ru.smirnov;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    private static String clientName = "";
    private static boolean isRunning = false;
    private static int PORT;
    private static Logger logger;


    public static void main(String[] args) throws IOException {
        SettingsLoader settings = new SettingsLoader("setting.settings.properties");
        PORT = settings.getPort();
        logger = Logger.getInstance();

        try (Socket socket = new Socket("127.0.0.1", PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             Scanner sc = new Scanner(System.in)
        ) {

            while (!isRunning) {
                logger.logAndPrint("newClient", ("Введите имя:"));
                clientName = sc.nextLine();
                logger.log("newClient", clientName);
                Message.sendMsg(out, clientName, MessageTypes.MT_INIT, "");
                try {
                    Message receiveMessage = (Message) in.readObject();
                    if (receiveMessage.type == MessageTypes.MT_ERROR) {
                        logger.logAndPrint("newClient", "Это имя уже занято");
                        continue;
                    }
                    isRunning = true;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            Thread proccesServer = getThread(out, in);

            //Thread.sleep(1000);
            while (isRunning) {
                String data = sc.nextLine();
                if (data.isEmpty()) continue;
                logger.log(clientName, data);
                if (data.equals("/exit")) {
                    Message.sendMsg(out, clientName, MessageTypes.MT_EXIT, "");
                    isRunning = false;
                    break;
                }
                else {
                    Message.sendMsg(out, clientName, MessageTypes.MT_DATA, data);
                }
            }

            proccesServer.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Thread getThread(ObjectOutputStream out, ObjectInputStream in) {
        Thread proccesServer = new Thread(() -> {
            try {
                while (isRunning) {
                    Message.sendMsg(out, clientName, MessageTypes.MT_GETDATA, "");
                    Message receiveMessage = (Message) in.readObject();
                    switch (receiveMessage.type) {
                        case MT_DATA:
                            logger.logAndPrint(clientName, (receiveMessage.from + ": " + receiveMessage.data));
                            break;
                        default:
                            Thread.sleep(500);
                    }
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        proccesServer.start();
        return proccesServer;
    }
}
