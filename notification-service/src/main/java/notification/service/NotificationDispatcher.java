package notification.service;

import notification.dto.NotificationMessage;
import notification.entity.NotificationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationLogService logService;
    private final EmailService emailService;

    public NotificationDispatcher(NotificationLogService logService,
                                  EmailService emailService) {
        this.logService = logService;
        this.emailService = emailService;
    }

    public void dispatch(NotificationMessage message) {
        log.info("Dispatching notification: type={}, to={}", message.type(), message.recipientEmail());

        NotificationLog logEntry = logService.save(message);

        try {
            emailService.send(message);
            logService.updateStatus(logEntry.getId(), "SENT");
        } catch (Exception e) {
            log.error("Email sending failed for notification id={}", logEntry.getId(), e);
            logService.updateStatus(logEntry.getId(), "FAILED");
            throw e;
        }

        logService.updateStatus(logEntry.getId(), "PROCESSED");
        log.info("Notification {} processed", logEntry.getId());
    }
}