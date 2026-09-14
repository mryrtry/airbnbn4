package notification.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import notification.dto.NotificationMessage;
import notification.service.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class NotificationConsumer {

    public static final String NOTIFICATIONS_QUEUE = "notifications.queue";
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = NOTIFICATIONS_QUEUE)
    public void onMessage(byte[] body) {
        try {
            String json = new String(body, StandardCharsets.UTF_8);
            NotificationMessage message = objectMapper.readValue(json, NotificationMessage.class);
            dispatcher.dispatch(message);
        } catch (Exception e) {
            log.error("Failed to process notification", e);
            throw new RuntimeException(e);
        }
    }
}