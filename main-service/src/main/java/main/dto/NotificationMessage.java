package main.dto;

import java.io.Serializable;

public record NotificationMessage(
        String type,
        String recipientEmail,
        String recipientName,
        String subject,
        String body
) implements Serializable {
}