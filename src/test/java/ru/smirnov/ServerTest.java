package ru.smirnov;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerTest {

    private static int PORT = 8080;
    private static Server server;
    private static Logger mockLogger;

    @BeforeAll
    static void prepareServer() {
        mockLogger = mock(Logger.class);
        mockLoggerSingleton(mockLogger);
        server = new Server(PORT);
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

    @BeforeEach
    void cleanUpServer() {
        server.getSessionMap().clear();
    }

    private void runTask(Socket socket) throws IOException {
        Runnable clientHandler = server.processClient(socket);
        try {
            clientHandler.run();
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof EOFException)) {
                throw e;
            }
        }
    }

    private Socket getPreparedClientSocket(Message... messages) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        for (Message msg : messages) {
            oos.writeObject(msg);
        }
        oos.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ByteArrayOutputStream outBaos = new ByteArrayOutputStream();

        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(bais);
        when(socket.getOutputStream()).thenReturn(outBaos);

        return socket;
    }

    private List<Message> readResponses(ByteArrayOutputStream outBaos) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(outBaos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        List<Message> result = new ArrayList<>();
        while (bais.available() > 0) {
            result.add((Message) ois.readObject());
        }
        return result;
    }

    @Test
    void testMT_INIT_Success() throws Exception {
        Message init = new Message("client1", MessageTypes.MT_INIT, "");
        Socket socket = getPreparedClientSocket(init);
        ByteArrayOutputStream outBaos = (ByteArrayOutputStream) socket.getOutputStream();

        runTask(socket);

        assertTrue(server.getSessionMap().containsKey("client1"));
        List<Message> response = readResponses(outBaos);
        assertEquals(1, response.size());
        assertEquals(MessageTypes.MT_CONFIRM, response.get(0).type);
    }

    @Test
    void testMT_EXIT_Success() throws Exception {
        Message init = new Message("client1", MessageTypes.MT_INIT, "");
        Message exit = new Message("client1", MessageTypes.MT_EXIT, "");
        Socket socket = getPreparedClientSocket(init, exit);

        runTask(socket);

        assertFalse(server.getSessionMap().containsKey("client1"));
        verify(socket, atLeastOnce()).close();
    }

    @Test
    void testMT_DATA_Success() throws Exception {
        Message data = new Message("client1", MessageTypes.MT_DATA, "Hello");

        Session session1 = mock(Session.class);
        Session session2 = mock(Session.class);
        doNothing().when(session1).pushMessage(any());
        doNothing().when(session2).pushMessage(any());

        server.getSessionMap().put("client1", session1);
        server.getSessionMap().put("client2", session2);

        Socket socket = getPreparedClientSocket(data);

        runTask(socket);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(session1).pushMessage(captor.capture());
        verify(session2).pushMessage(captor.capture());

        List<Message> msgs = captor.getAllValues();
        assertEquals(2, msgs.size());
        assertEquals("Hello", msgs.get(0).data);
    }

    @Test
    void testMT_GETDATA_Success() throws Exception {
        Message get = new Message("client1", MessageTypes.MT_GETDATA, "");

        Session session = mock(Session.class);
        when(session.getMessage()).thenReturn(new Message("server", MessageTypes.MT_DATA, "response"));

        server.getSessionMap().put("client1", session);

        Socket socket = getPreparedClientSocket(get);
        runTask(socket);

        verify(session).getMessage();
    }

    @Test
    void testMT_GETDATA_MultipleTimes() throws Exception {
        Message get1 = new Message("client1", MessageTypes.MT_GETDATA, "");
        Message get2 = new Message("client1", MessageTypes.MT_GETDATA, "");
        Message get3 = new Message("client1", MessageTypes.MT_GETDATA, "");

        Session session = mock(Session.class);
        Message response = new Message("server", MessageTypes.MT_DATA, "multi");
        when(session.getMessage()).thenReturn(response);

        server.getSessionMap().put("client1", session);

        Socket socket = getPreparedClientSocket(get1, get2, get3);
        ByteArrayOutputStream outBaos = (ByteArrayOutputStream) socket.getOutputStream();

        runTask(socket);
        List<Message> msgs = readResponses(outBaos);

        assertEquals(3, msgs.size());
        for (Message msg : msgs) {
            assertEquals(response, msg);
        }
    }

    static Stream<Arguments> provideInvalidNames() {
        return Stream.of(
                Arguments.of(new Message("", MessageTypes.MT_INIT, "")),
                Arguments.of(new Message("server", MessageTypes.MT_INIT, "")),
                Arguments.of(new Message("client1", MessageTypes.MT_INIT, ""))
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidNames")
    void testMT_INIT_DuplicateOrInvalidName(Message msg) throws Exception {
        server.getSessionMap().put("client1", mock(Session.class));

        Socket socket = getPreparedClientSocket(msg);
        ByteArrayOutputStream outBaos = (ByteArrayOutputStream) socket.getOutputStream();

        runTask(socket);
        List<Message> response = readResponses(outBaos);

        assertEquals(1, response.size());
        assertEquals(new Message("server", MessageTypes.MT_ERROR, "Это имя уже занято"), response.get(0));
    }
}

