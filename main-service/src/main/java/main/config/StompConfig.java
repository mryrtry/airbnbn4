package main.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.producer.StompNotificationSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StompConfig {

    @Bean
    public StompNotificationSender stompNotificationSender(ObjectMapper objectMapper) throws Exception {
        StompNotificationSender sender = new StompNotificationSender(
                "localhost",
                61613,
                "admin",
                "admin",
                "/queue/notifications.queue",
                objectMapper
        );
        sender.connect();
        return sender;
    }
}