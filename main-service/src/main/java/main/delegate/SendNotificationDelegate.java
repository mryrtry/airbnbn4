package main.delegate;

import main.dto.NotificationMessage;
import main.producer.NotificationProducer;
import main.util.CamundaVars;
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
        String type = CamundaVars.getString(execution, "notificationType");
        String email = CamundaVars.getString(execution, "notificationRecipientEmail");
        String name = CamundaVars.getString(execution, "notificationRecipientName");
        String subject = CamundaVars.getString(execution, "notificationSubject");
        String body = CamundaVars.getString(execution, "notificationBody");

        if (email == null || email.isBlank()) {
            log.warn("Skipping notification type={}: recipient email is missing", type);
            return;
        }

        notificationProducer.send(new NotificationMessage(type, email, name, subject, body));
        log.info("Notification sent: type={}, to={}", type, email);
    }
}