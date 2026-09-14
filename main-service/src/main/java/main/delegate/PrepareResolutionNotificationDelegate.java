package main.delegate;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareResolutionNotificationDelegate")
public class PrepareResolutionNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareResolutionNotificationDelegate.class);

    private final IdentityService identityService;

    public PrepareResolutionNotificationDelegate(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String type = (String) execution.getVariable("notificationType");
        String recipient = (String) execution.getVariable("notificationRecipient");

        // Фолбэк по activityId
        if (type == null) {
            type = switch (execution.getCurrentActivityId()) {
                case "Task_PrepareNotifyWindowOpened"     -> "RESOLUTION_WINDOW_OPENED";
                case "Task_PrepareNotifyExpired"          -> "RESOLUTION_WINDOW_EXPIRED";
                case "Task_PrepareNotifyComplaintFiled"   -> "RESOLUTION_FILED";
                case "Task_PrepareNotifyApprovedGuest",
                     "Task_PrepareNotifyApprovedOwner"    -> "RESOLUTION_APPROVED";
                case "Task_PrepareNotifyPaidGuest",
                     "Task_PrepareNotifyPaidOwner"        -> "RESOLUTION_PAID";
                case "Task_PrepareNotifyDeclinedGuest",
                     "Task_PrepareNotifyDeclinedOwner"    -> "RESOLUTION_DECLINED";
                case "Task_PrepareNotifyRejectedGuest",
                     "Task_PrepareNotifyRejectedOwner"    -> "RESOLUTION_REJECTED";
                default -> null;
            };
        }
        if (type == null) {
            log.warn("Cannot resolve notificationType for activity {}", execution.getCurrentActivityId());
            return;
        }

        if (recipient == null) {
            recipient = switch (type) {
                case "RESOLUTION_WINDOW_OPENED",
                     "RESOLUTION_WINDOW_EXPIRED" -> "OWNER";
                case "RESOLUTION_FILED",
                     "RESOLUTION_APPROVED",
                     "RESOLUTION_PAID",
                     "RESOLUTION_DECLINED",
                     "RESOLUTION_REJECTED" -> "GUEST";
                default -> null;
            };
        }

        String ownerId = (String) execution.getVariable("ownerId");
        String guestId = (String) execution.getVariable("guestId");
        String recipientUser = "GUEST".equals(recipient) ? guestId : ownerId;

        String recipientEmail = getEmail(recipientUser);
        String subject;
        String body;

        Object bookingId = execution.getVariable("bookingId");
        Object reason = execution.getVariable("reason");
        Object adminComment = execution.getVariable("adminComment");

        String dates = execution.getVariable("bookingStart") + " — " + execution.getVariable("bookingEnd");

        switch (type + ":" + recipient) {
            case "RESOLUTION_WINDOW_OPENED:OWNER" -> {
                subject = "Окно резолюции открыто";
                body = String.format(
                        "Здравствуйте!%n%nГость выехал из вашего листинга.%n%n" +
                                "Бронь: %s%nДаты: %s%n%n" +
                                "Открыто окно резолюции на 1 минуту. " +
                                "Вы можете подать жалобу в Tasklist.%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId, dates);
            }
            case "RESOLUTION_WINDOW_EXPIRED:OWNER" -> {
                subject = "Окно резолюции закрыто";
                body = String.format(
                        "Здравствуйте!%n%nОкно резолюции по брони %s закрыто — жалоба не была подана.%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId);
            }
            case "RESOLUTION_FILED:GUEST" -> {
                subject = "Подана жалоба";
                body = String.format(
                        "Здравствуйте!%n%nВладелец подал жалобу по вашей брони %s.%n%n" +
                                "Причина: %s%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId, reason != null ? reason : "—");
            }
            case "RESOLUTION_APPROVED:GUEST" -> {
                subject = "Жалоба одобрена";
                body = String.format(
                        "Здравствуйте!%n%nПо жалобе принято решение: одобрено.%n%n" +
                                "Бронь: %s%n%n" +
                                "Зайдите в Tasklist, чтобы оплатить компенсацию или отказаться.%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId);
            }
            case "RESOLUTION_APPROVED:OWNER" -> {
                subject = "Ваша жалоба одобрена";
                body = String.format(
                        "Здравствуйте!%n%nВаша жалоба по брони %s одобрена администратором.%n%n" +
                                "Ожидается оплата компенсации гостем.%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId);
            }
            case "RESOLUTION_REJECTED:GUEST" -> {
                subject = "Жалоба отклонена";
                body = String.format(
                        "Здравствуйте!%n%nЖалоба по вашей брони %s отклонена администратором.%n%n" +
                                "Комментарий: %s%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId, adminComment != null ? adminComment : "—");
            }
            case "RESOLUTION_REJECTED:OWNER" -> {
                subject = "Жалоба отклонена";
                body = String.format(
                        "Здравствуйте!%n%nВаша жалоба по брони %s отклонена администратором.%n%n" +
                                "Комментарий: %s%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId, adminComment != null ? adminComment : "—");
            }
            case "RESOLUTION_PAID:GUEST" -> {
                subject = "Компенсация оплачена";
                body = String.format(
                        "Здравствуйте!%n%nВы оплатили компенсацию по жалобе на бронь %s.%n%n" +
                                "Спасибо!%n%nС уважением,%nAirbnb BPM",
                        bookingId);
            }
            case "RESOLUTION_PAID:OWNER" -> {
                subject = "Компенсация получена";
                body = String.format(
                        "Здравствуйте!%n%nГость оплатил компенсацию по жалобе на бронь %s.%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId);
            }
            case "RESOLUTION_DECLINED:GUEST" -> {
                subject = "Вы отказались от оплаты";
                body = String.format(
                        "Здравствуйте!%n%nВы отказались оплачивать компенсацию по жалобе на бронь %s.%n%n" +
                                "Ваш аккаунт заблокирован.%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId);
            }
            case "RESOLUTION_DECLINED:OWNER" -> {
                subject = "Гость отказался платить";
                body = String.format(
                        "Здравствуйте!%n%nГость отказался оплатить компенсацию по жалобе на бронь %s.%n%n" +
                                "Его аккаунт заблокирован.%n%n" +
                                "С уважением,%nAirbnb BPM",
                        bookingId);
            }
            default -> {
                log.warn("Unknown notification combination type={}, recipient={}", type, recipient);
                return;
            }
        }

        execution.setVariable("notificationRecipientEmail", recipientEmail);
        execution.setVariable("notificationRecipientName", recipientUser);
        execution.setVariable("notificationSubject", subject);
        execution.setVariable("notificationBody", body);

        log.info("[RESOLUTION] Prepared notification type={}, recipient={}, to={}",
                type, recipient, recipientEmail);
    }

    private String getEmail(String username) {
        if (username == null) return null;
        var user = identityService.createUserQuery().userId(username).singleResult();
        return user != null ? user.getEmail() : null;
    }
}