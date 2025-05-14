package ru.smirnov;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    private static int clientID = 0;
    private static boolean isRunning = false;

    public static void main(String[] args) throws IOException {
        try {
            Socket socket = new Socket("127.0.0.1", 8080);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Scanner sc = new Scanner(System.in);
            Message connectMessage = new Message(clientID, MessageTypes.MT_INIT);
            out.writeObject(connectMessage);
            out.flush();
            try {
                Message receiveMessage = (Message) in.readObject();
                clientID = Integer.parseInt(receiveMessage.data);
                isRunning = true;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            Thread proccesServer = getThread(out, in);

            Thread.sleep(1000);
            while (isRunning) {
                String data = "efewf";
                if (data.isEmpty()) continue;
                Message sendMessage;
                if (data.equals("/exit")) {
                    sendMessage = new Message(clientID, MessageTypes.MT_EXIT);
                    synchronized (out) {
                        out.writeObject(sendMessage);
                        out.flush();
                    }
                    isRunning = false;
                    break;
                }
                else {
                    sendMessage = new Message(clientID, MessageTypes.MT_DATA, data);
                }
                synchronized (out) {
                    out.writeObject(sendMessage);
                    out.flush();
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
                    Message sendMsg = new Message(clientID, MessageTypes.MT_GETDATA);
                    synchronized (out) {
                        out.writeObject(sendMsg);
                        out.flush();
                    }
                    Message receiveMessage = (Message) in.readObject();
                    switch (receiveMessage.type) {
                        case MT_DATA:
                            System.out.println(receiveMessage.from + ": " + receiveMessage.data);
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