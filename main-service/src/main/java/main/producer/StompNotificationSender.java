package main.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.dto.NotificationMessage;
import main.exception.ProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class StompNotificationSender implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(StompNotificationSender.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String destination;
    private final ObjectMapper objectMapper;

    private Socket socket;
    private OutputStream out;

    public StompNotificationSender(String host, int port, String username, String password,
                                   String destination, ObjectMapper objectMapper) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.destination = destination;
        this.objectMapper = objectMapper;
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            out = socket.getOutputStream();

            String connectFrame = "CONNECT\n"
                    + "accept-version:1.2\n"
                    + "host:localhost\n"
                    + "login:" + username + "\n"
                    + "passcode:" + password + "\n"
                    + "\n\0";

            out.write(connectFrame.getBytes(StandardCharsets.UTF_8));
            out.flush();
            log.info("STOMP sender connected to {}:{}", host, port);

        } catch (Exception ex) {
            throw new ProcessException("Failed to connect STOMP sender", ex);
        }
    }

    public synchronized void send(NotificationMessage message) {
        try {
            ensureConnected();

            String json = objectMapper.writeValueAsString(message);
            byte[] body = json.getBytes(StandardCharsets.UTF_8);

            String frame = "SEND\n"
                    + "destination:" + destination + "\n"
                    + "content-type:application/json\n"
                    + "content-length:" + body.length + "\n"
                    + "persistent:true\n"
                    + "\n"
                    + json + "\0";

            out.write(frame.getBytes(StandardCharsets.UTF_8));
            out.flush();

            log.info("STOMP message sent: type={}, to={}",
                    message.type(), message.recipientEmail());

        } catch (Exception ex) {
            throw new ProcessException("Failed to send STOMP message", ex);
        }
    }

    private void ensureConnected() throws Exception {
        if (socket == null || socket.isClosed() || out == null) {
            connect();
        }
    }

    @Override
    public synchronized void destroy() {
        if (socket == null || socket.isClosed()) {
            return;
        }
        try {
            out.write("DISCONNECT\n\n\0".getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception ex) {
            log.debug("Failed to send STOMP DISCONNECT: {}", ex.getMessage());
        }
        try {
            socket.close();
        } catch (Exception ex) {
            log.debug("Failed to close STOMP socket: {}", ex.getMessage());
        }
    }
}