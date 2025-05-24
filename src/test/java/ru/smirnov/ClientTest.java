package ru.smirnov;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

class ClientTest {

    private static int PORT = 9999;
    private static Client client;
    private static Logger mockLogger;

    @BeforeAll
    static void prepareClient() {
        mockLogger = mock(Logger.class);
        mockLoggerSingleton(mockLogger);
    }

    private static void mockLoggerSingleton(Logger mockLogger) {
        try {
            Field instanceField = Logger.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, mockLogger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getPreparedClient(Message... messages) throws IOException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        client = new Client(PORT);
        ByteArrayOutputStream dummyOosBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        ObjectOutputStream tmpOos = new ObjectOutputStream(tmp);

        for (Message msg : messages) {
            tmpOos.writeObject(msg);
        }
        tmpOos.flush();
        ByteArrayInputStream dummyOisBais = new ByteArrayInputStream(tmp.toByteArray());

        Field fOos = Client.class.getDeclaredField("oos");
        Field fOis = Client.class.getDeclaredField("ois");
        fOos.setAccessible(true);
        fOis.setAccessible(true);
        fOos.set(client, new ObjectOutputStream(dummyOosBaos));
        fOis.set(client, new ObjectInputStream(dummyOisBais));
    }


    @Test
    void testMT_INIT() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, NoSuchFieldException {
        Message confirm = new Message("server", MessageTypes.MT_CONFIRM, "");
        getPreparedClient(confirm);

        Method init = Client.class.getDeclaredMethod("init", String.class);
        init.setAccessible(true);
        init.invoke(client, "bob");

        String expectedClientName = "bob";
        boolean expectedIsRunning = true;

        assertEquals(expectedClientName, client.getClientName());
        assertEquals(expectedIsRunning, client.getIsRunning());
    }


    @Test
    void testMT_INIT_Failure() throws Exception {
        Message error = new Message("server", MessageTypes.MT_ERROR, "");
        getPreparedClient(error);

        Method init = Client.class.getDeclaredMethod("init", String.class);
        init.setAccessible(true);
        init.invoke(client, "bob");

        String expectedClientName = "";
        boolean expectedIsRunning = false;

        assertEquals(expectedClientName, client.getClientName());
        assertEquals(expectedIsRunning, client.getIsRunning());
    }

}
