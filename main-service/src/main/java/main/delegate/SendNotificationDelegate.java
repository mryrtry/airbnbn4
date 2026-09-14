package main.delegate;

import main.dto.NotificationMessage;
import main.producer.NotificationProducer;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("sendNotificationDelegate")
public class SendNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationDelegate.class);

    private final NotificationProducer notificationProducer;

    public SendNotificationDelegate(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String type = (String) execution.getVariable("notificationType");
        String email = (String) execution.getVariable("notificationRecipientEmail");
        String name = (String) execution.getVariable("notificationRecipientName");
        String subject = (String) execution.getVariable("notificationSubject");
        String body = (String) execution.getVariable("notificationBody");

        NotificationMessage message = new NotificationMessage(type, email, name, subject, body);
        notificationProducer.send(message);

        log.info("Notification sent: type={}, to={}", type, email);
    }
}