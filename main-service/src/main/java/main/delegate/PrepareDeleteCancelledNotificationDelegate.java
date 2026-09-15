package main.delegate;

import main.service.NotificationContextService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareDeleteCancelledNotificationDelegate")
public class PrepareDeleteCancelledNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareDeleteCancelledNotificationDelegate.class);

    private final NotificationContextService contextService;

    public PrepareDeleteCancelledNotificationDelegate(NotificationContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String initiator = CamundaVars.getString(execution, "initiatorUserId");
        String email = CamundaVars.getString(execution, "initiatorEmail");
        if (email == null || email.isBlank()) {
            email = contextService.resolveEmail(initiator);
        }

        Long listingId = CamundaVars.getLong(execution, "listingId");
        String listingTitle = CamundaVars.getString(execution, "listingTitle");
        String listingAddress = CamundaVars.getString(execution, "listingAddress");

        execution.setVariable("notificationType", "LISTING_DELETE_CANCELLED");
        execution.setVariable("notificationRecipientEmail", email);
        execution.setVariable("notificationRecipientName", initiator);
        execution.setVariable("notificationSubject", "Удаление объявления отменено");
        execution.setVariable("notificationBody", String.format(
                "Здравствуйте!%n%nВы отменили удаление объявления.%n%n" +
                        "ID: %s%nНазвание: %s%nАдрес: %s%n%n" +
                        "Объявление осталось активным и доступным для бронирования.%n%n" +
                        "С уважением,%nAirbnb BPM",
                listingId,
                valueOrDash(listingTitle),
                valueOrDash(listingAddress)));

        log.info("Prepared LISTING_DELETE_CANCELLED notification for listing id={}", listingId);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}