package ru.smirnov;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class Session {
    private static final int DEQUE_CAPASITY = 50;
    private final BlockingDeque<Message> deque;

    public Session() {
        deque = new LinkedBlockingDeque<>();
    }

    public Session(int size) {
        deque = new LinkedBlockingDeque<>(size);
    }

    public void pushMessage(Message msg) throws InterruptedException {
        deque.put(msg);
    }

    public Message getMessage() throws InterruptedException {
        if (!deque.isEmpty()) return deque.take();
        return new Message(-1, MessageTypes.MT_NODATA);
    }


}
