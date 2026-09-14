package main.delegate;

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
        Object listingIdObj = execution.getVariable("listingId");
        Long listingId = listingIdObj == null ? null : ((Number) listingIdObj).longValue();
        String title = (String) execution.getVariable("title");
        String address = (String) execution.getVariable("address");

        execution.setVariable("notificationType", "LISTING_CREATED");
        execution.setVariable("notificationSubject", "Объявление создано: " + title);
        execution.setVariable("notificationBody", String.format(
                "Здравствуйте!%n%nВаше объявление успешно создано и опубликовано.%n%n" +
                        "ID: %s%nНазвание: %s%nАдрес: %s%n%nС уважением,%nAirbnb BPM",
                listingId, title, address));

        log.info("Prepared LISTING_CREATED notification for listing id={}", listingId);
    }
}