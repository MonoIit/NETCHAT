package ru.smirnov;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class Session {
    private static final int DEQUE_CAPASITY = Integer.MAX_VALUE;
    private final BlockingDeque<Message> deque;

    public Session() {
        deque = new LinkedBlockingDeque<>(DEQUE_CAPASITY);
    }

    public Session(int size) {
        deque = new LinkedBlockingDeque<>(size);
    }

    public void pushMessage(Message msg) throws InterruptedException {
        deque.put(msg);
    }

    public Message getMessage() throws InterruptedException {
        if (!deque.isEmpty()) return deque.take();
        return new Message("server", MessageTypes.MT_NODATA);
    }


}
