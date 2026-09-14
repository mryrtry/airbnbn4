package main.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareListingFailedNotificationDelegate")
public class PrepareListingFailedNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareListingFailedNotificationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String error = (String) execution.getVariable("createError");
        String title = (String) execution.getVariable("title");

        execution.setVariable("notificationType", "LISTING_CREATE_FAILED");
        execution.setVariable("notificationSubject", "Ошибка создания объявления: " + title);
        execution.setVariable("notificationBody", String.format(
                "Здравствуйте!%n%nНе удалось создать объявление.%n%n" +
                        "Название: %s%nПричина: %s%n%n" +
                        "Попробуйте ещё раз или обратитесь в поддержку.%n%nС уважением,%nAirbnb BPM",
                title, error));

        log.info("Prepared LISTING_CREATE_FAILED notification: {}", error);
    }
}