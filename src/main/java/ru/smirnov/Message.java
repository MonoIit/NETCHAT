package ru.smirnov;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    String from;
    MessageTypes type;
    String data;

    public Message(String from, MessageTypes type, String data) {
        this.from = from;
        this.type = type;
        this.data = data;
    }

    public Message(String from, MessageTypes type) {
        this(from, type, "");
    }

    @Override
    public String toString() {
        return from + ";" + type + ";" + data;
    }

    public static void sendMsg(ObjectOutputStream out, String from, MessageTypes type, String data) throws IOException {
        synchronized (out) {
            out.writeObject(new Message(from, type, data));
            out.flush();
        }
    }

    public void send(ObjectOutputStream out) throws IOException {
        synchronized (out) {
            out.writeObject(this);
            out.flush();
        }
    }
}
