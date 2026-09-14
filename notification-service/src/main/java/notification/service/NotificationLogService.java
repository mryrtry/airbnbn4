package notification.service;

import notification.dto.NotificationMessage;
import notification.entity.NotificationLog;
import notification.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationLogService {

    private final NotificationLogRepository repository;

    public NotificationLogService(NotificationLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationLog save(NotificationMessage message) {
        NotificationLog entity = new NotificationLog();
        entity.setType(message.type());
        entity.setRecipientEmail(message.recipientEmail());
        entity.setRecipientName(message.recipientName());
        entity.setSubject(message.subject());
        entity.setBody(message.body());
        entity.setReceivedAt(Instant.now());
        entity.setStatus("RECEIVED");
        return repository.save(entity);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        repository.findById(id).ifPresent(entity -> {
            entity.setStatus(status);
            repository.save(entity);
        });
    }
}