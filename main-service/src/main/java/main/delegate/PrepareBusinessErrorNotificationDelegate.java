package main.delegate;

import main.service.NotificationContextService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareBusinessErrorNotificationDelegate")
public class PrepareBusinessErrorNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareBusinessErrorNotificationDelegate.class);

    private final NotificationContextService contextService;

    public PrepareBusinessErrorNotificationDelegate(NotificationContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String processKey = processKey(execution.getProcessDefinitionId());
        String operation = operationName(processKey);
        String errorCode = valueOrDefault(
                CamundaVars.getString(execution, "processErrorCode"),
                "BUSINESS_ERROR");
        String errorMessage = valueOrDefault(
                CamundaVars.getString(execution, "processErrorMessage"),
                "Операция не может быть выполнена.");
        String initiator = CamundaVars.getString(execution, "initiatorUserId");
        String email = CamundaVars.getString(execution, "initiatorEmail");
        if (email == null || email.isBlank()) {
            email = contextService.resolveEmail(initiator);
        }
        Object listingId = execution.getVariable("listingId");
        String listingDetails = listingId == null ? "" : String.format("%nID объявления: %s", listingId);

        execution.setVariable("notificationType", "PROCESS_BUSINESS_ERROR");
        execution.setVariable("notificationRecipientEmail", email);
        execution.setVariable("notificationRecipientName", initiator);
        execution.setVariable("notificationSubject", "Не удалось выполнить операцию: " + operation);
        execution.setVariable("notificationBody", String.format(
                "Здравствуйте!%n%nОперация «%s» не выполнена.%n%n" +
                        "Причина: %s%nКод ошибки: %s%nID процесса: %s%s%n%n" +
                        "Проверьте введённые данные и попробуйте ещё раз.%n%n" +
                        "С уважением,%nAirbnb BPM",
                operation, errorMessage, errorCode, execution.getProcessInstanceId(), listingDetails));

        log.info("Prepared business error notification: process={}, code={}, initiator={}",
                processKey, errorCode, initiator);
    }

    private String processKey(String processDefinitionId) {
        if (processDefinitionId == null || processDefinitionId.isBlank()) {
            return "unknown";
        }
        int separator = processDefinitionId.indexOf(':');
        return separator >= 0 ? processDefinitionId.substring(0, separator) : processDefinitionId;
    }

    private String operationName(String processKey) {
        return switch (processKey) {
            case "booking-lifecycle" -> "Создание бронирования";
            case "listing-create" -> "Создание объявления";
            case "listing-update" -> "Редактирование объявления";
            case "listing-delete" -> "Удаление объявления";
            default -> "Выполнение процесса";
        };
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
