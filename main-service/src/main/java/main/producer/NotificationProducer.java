package main.producer;

import main.dto.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final StompNotificationSender sender;

    public NotificationProducer(StompNotificationSender sender) {
        this.sender = sender;
    }

    public void send(NotificationMessage message) {
        try {
            sender.send(message);
        } catch (Exception e) {
            log.error("Failed to send STOMP message", e);
            throw new RuntimeException(e);
        }
    }
}