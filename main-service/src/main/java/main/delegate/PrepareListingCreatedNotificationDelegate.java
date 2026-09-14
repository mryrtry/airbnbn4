package main.delegate;

import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareListingCreatedNotificationDelegate")
public class PrepareListingCreatedNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareListingCreatedNotificationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        Long listingId = CamundaVars.getLong(execution, "listingId");
        String title = CamundaVars.getString(execution, "title");
        String address = CamundaVars.getString(execution, "address");

        execution.setVariable("notificationType", "LISTING_CREATED");
        execution.setVariable("notificationSubject", "Объявление создано: " + title);
        execution.setVariable("notificationBody", String.format(
                "Здравствуйте!%n%nВаше объявление успешно создано и опубликовано.%n%n" +
                        "ID: %s%nНазвание: %s%nАдрес: %s%n%nС уважением,%nAirbnb BPM",
                listingId, title, address));

        log.info("Prepared LISTING_CREATED notification for listing id={}", listingId);
    }
}