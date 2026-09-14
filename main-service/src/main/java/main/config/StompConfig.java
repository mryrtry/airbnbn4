package main.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.producer.StompNotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StompConfig {

    @Bean
    public StompNotificationSender stompNotificationSender(
            ObjectMapper objectMapper,
            @Value("${app.stomp.host:localhost}") String host,
            @Value("${app.stomp.port:61613}") int port,
            @Value("${app.stomp.username:admin}") String username,
            @Value("${app.stomp.password:admin}") String password,
            @Value("${app.stomp.destination:/queue/notifications.queue}") String destination) {

        StompNotificationSender sender = new StompNotificationSender(
                host, port, username, password, destination, objectMapper);
        sender.connect();
        return sender;
    }
}