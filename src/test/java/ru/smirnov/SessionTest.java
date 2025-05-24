package ru.smirnov;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Test
    void testPushAndGetMessage() throws InterruptedException {
        Session session = new Session();
        Message msg = new Message("user1", MessageTypes.MT_DATA, "Hello");

        session.pushMessage(msg);
        Message received = session.getMessage();

        assertEquals("user1", received.from);
        assertEquals(MessageTypes.MT_DATA, received.type);
        assertEquals("Hello", received.data);
    }

    @Test
    void testGetMessageWhenEmpty() throws InterruptedException {
        Session session = new Session();

        Message received = session.getMessage();

        assertEquals("server", received.from);
        assertEquals(MessageTypes.MT_NODATA, received.type);
    }

    @Test
    void testMessageOrderFIFO() throws InterruptedException {
        Session session = new Session();
        Message msg1 = new Message("user", MessageTypes.MT_DATA, "First");
        Message msg2 = new Message("user", MessageTypes.MT_DATA, "Second");

        session.pushMessage(msg1);
        session.pushMessage(msg2);

        Message first = session.getMessage();
        Message second = session.getMessage();

        assertEquals("First", first.data);
        assertEquals("Second", second.data);
    }

    @Test
    void testLimitedCapacityBlocksOrThrows() throws InterruptedException {
        int capacity = 1;
        Session session = new Session(capacity);
        Message msg1 = new Message("user", MessageTypes.MT_DATA, "One");
        Message msg2 = new Message("user", MessageTypes.MT_DATA, "Two");

        session.pushMessage(msg1);

        Thread t = new Thread(() -> {
            try {
                session.pushMessage(msg2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t.start();


        Thread.sleep(100);

        assertTrue(t.isAlive());

        session.getMessage();

        t.join(1000);
        assertFalse(t.isAlive());
    }
}

