package ru.smirnov;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    int from;
    MessageTypes type;
    String data;

    public Message(int from, MessageTypes type, String data) {
        this.from = from;
        this.type = type;
        this.data = data;
    }

    public Message(int from, MessageTypes type) {
        this(from, type, "");
    }

    @Override
    public String toString() {
        return from + ";" + type + ";" + data;
    }


}
