package main.service;

import main.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class ResolutionNotificationComposer {

    public String subject(String type, String recipient) {
        return switch (key(type, recipient)) {
            case "RESOLUTION_WINDOW_OPENED:OWNER" -> "Окно резолюции открыто";
            case "RESOLUTION_WINDOW_EXPIRED:OWNER" -> "Окно резолюции закрыто";
            case "RESOLUTION_FILED:GUEST" -> "Подана жалоба";
            case "RESOLUTION_APPROVED:GUEST" -> "Жалоба одобрена";
            case "RESOLUTION_APPROVED:OWNER" -> "Ваша жалоба одобрена";
            case "RESOLUTION_REJECTED:GUEST" -> "Жалоба отклонена";
            case "RESOLUTION_REJECTED:OWNER" -> "Жалоба отклонена";
            case "RESOLUTION_PAID:GUEST" -> "Компенсация оплачена";
            case "RESOLUTION_PAID:OWNER" -> "Компенсация получена";
            case "RESOLUTION_DECLINED:GUEST" -> "Вы отказались от оплаты";
            case "RESOLUTION_DECLINED:OWNER" -> "Гость отказался платить";
            default -> throw unknown(type, recipient);
        };
    }

    public String body(String type, String recipient,
                       Object bookingId, Object reason, Object adminComment,
                       String dates) {

        return switch (key(type, recipient)) {
            case "RESOLUTION_WINDOW_OPENED:OWNER" -> String.format(
                    "Здравствуйте!%n%nГость выехал из вашего листинга.%n%n" +
                            "Бронь: %s%nДаты: %s%n%n" +
                            "Открыто окно резолюции на 1 минуту. " +
                            "Вы можете подать жалобу в Tasklist.%n%n" +
                            "С уважением,%nAirbnb BPM",
                    bookingId, dates);

            case "RESOLUTION_WINDOW_EXPIRED:OWNER" -> String.format(
                    "Здравствуйте!%n%nОкно резолюции по брони %s закрыто — жалоба не была подана.%n%n" +
                            "С уважением,%nAirbnb BPM",
                    bookingId);

            case "RESOLUTION_FILED:GUEST" -> String.format(
                    "Здравствуйте!%n%nВладелец подал жалобу по вашей брони %s.%n%n" +
                            "Причина: %s%n%nС уважением,%nAirbnb BPM",
                    bookingId, orDash(reason));

            case "RESOLUTION_APPROVED:GUEST" -> String.format(
                    "Здравствуйте!%n%nПо жалобе принято решение: одобрено.%n%n" +
                            "Бронь: %s%n%nЗайдите в Tasklist, чтобы оплатить компенсацию или отказаться.%n%n" +
                            "С уважением,%nAirbnb BPM",
                    bookingId);

            case "RESOLUTION_APPROVED:OWNER" -> String.format(
                    "Здравствуйте!%n%nВаша жалоба по брони %s одобрена администратором.%n%n" +
                            "Ожидается оплата компенсации гостем.%n%nС уважением,%nAirbnb BPM",
                    bookingId);

            case "RESOLUTION_REJECTED:GUEST" -> String.format(
                    "Здравствуйте!%n%nЖалоба по вашей брони %s отклонена администратором.%n%n" +
                            "Комментарий: %s%n%nС уважением,%nAirbnb BPM",
                    bookingId, orDash(adminComment));

            case "RESOLUTION_REJECTED:OWNER" -> String.format(
                    "Здравствуйте!%n%nВаша жалоба по брони %s отклонена администратором.%n%n" +
                            "Комментарий: %s%n%nС уважением,%nAirbnb BPM",
                    bookingId, orDash(adminComment));

            case "RESOLUTION_PAID:GUEST" -> String.format(
                    "Здравствуйте!%n%nВы оплатили компенсацию по жалобе на бронь %s.%n%n" +
                            "Спасибо!%n%nС уважением,%nAirbnb BPM",
                    bookingId);

            case "RESOLUTION_PAID:OWNER" -> String.format(
                    "Здравствуйте!%n%nГость оплатил компенсацию по жалобе на бронь %s.%n%n" +
                            "С уважением,%nAirbnb BPM",
                    bookingId);

            case "RESOLUTION_DECLINED:GUEST" -> String.format(
                    "Здравствуйте!%n%nВы отказались оплачивать компенсацию по жалобе на бронь %s.%n%n" +
                            "Ваш аккаунт заблокирован.%n%nС уважением,%nAirbnb BPM",
                    bookingId);

            case "RESOLUTION_DECLINED:OWNER" -> String.format(
                    "Здравствуйте!%n%nГость отказался оплатить компенсацию по жалобе на бронь %s.%n%n" +
                            "Его аккаунт заблокирован.%n%nС уважением,%nAirbnb BPM",
                    bookingId);

            default -> throw unknown(type, recipient);
        };
    }

    private String key(String type, String recipient) {
        return type + ":" + recipient;
    }

    private ValidationException unknown(String type, String recipient) {
        return new ValidationException(
                "Unknown resolution notification type: " + key(type, recipient));
    }

    private String orDash(Object value) {
        if (value == null) {
            return "—";
        }
        String s = value.toString();
        return s.isBlank() ? "—" : s;
    }
}