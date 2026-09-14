package main.delegate;

import main.exception.ValidationException;
import main.service.NotificationContextService;
import main.service.ResolutionNotificationComposer;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareResolutionNotificationDelegate")
public class PrepareResolutionNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareResolutionNotificationDelegate.class);

    private final NotificationContextService contextService;
    private final ResolutionNotificationComposer composer;

    public PrepareResolutionNotificationDelegate(NotificationContextService contextService,
                                                 ResolutionNotificationComposer composer) {
        this.contextService = contextService;
        this.composer = composer;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String type = CamundaVars.getString(execution, "notificationType");
        String recipient = CamundaVars.getString(execution, "notificationRecipient");

        String guestId = CamundaVars.getString(execution, "guestId");
        String ownerId = CamundaVars.getString(execution, "ownerId");

        String recipientUser = contextService.resolveRecipient(recipient, guestId, ownerId);

        Object bookingId = execution.getVariable("bookingId");
        Object reason = execution.getVariable("reason");
        Object adminComment = execution.getVariable("adminComment");
        String dates = execution.getVariable("bookingStart") + " — "
                + execution.getVariable("bookingEnd");

        try {
            String subject = composer.subject(type, recipient);
            String body = composer.body(type, recipient, bookingId, reason, adminComment, dates);

            execution.setVariable("notificationRecipientEmail", contextService.resolveEmail(recipientUser));
            execution.setVariable("notificationRecipientName", recipientUser);
            execution.setVariable("notificationSubject", subject);
            execution.setVariable("notificationBody", body);

            log.info("[RESOLUTION] Prepared notification type={}, recipient={}, to={}",
                    type, recipient, recipientUser);

        } catch (ValidationException ex) {
            log.warn("Skipping notification: {}", ex.getMessage());
        }
    }
}