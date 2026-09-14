package main.service;

import main.entity.Booking;
import main.entity.Listing;
import main.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class BookingNotificationComposer {

    public String subject(String type, String recipient, Booking booking, Listing listing) {
        return switch (type + ":" + recipient) {
            case "BOOKING_CREATED:OWNER" -> "Новая заявка на бронирование";
            case "BOOKING_APPROVED:GUEST" -> "Бронь одобрена";
            case "BOOKING_REJECTED:GUEST" -> "Бронь отклонена";
            case "BOOKING_CHECKED_IN:GUEST" -> "Заезд подтверждён";
            case "BOOKING_CHECKED_IN:OWNER" -> "Гость заехал";
            case "BOOKING_CHECKED_OUT:GUEST" -> "Выезд подтверждён";
            case "BOOKING_CHECKED_OUT:OWNER" -> "Гость выехал";
            default -> throw new ValidationException(
                    "Unknown booking notification type: " + type + ":" + recipient);
        };
    }

    public String body(String type, String recipient, Booking booking, Listing listing) {
        String title = listing != null ? listing.getTitle() : "?";
        String dates = booking.getBookingStart() + " — " + booking.getBookingEnd();

        return switch (type + ":" + recipient) {
            case "BOOKING_CREATED:OWNER" -> String.format(
                    "Здравствуйте!%n%nПоступила новая заявка на бронирование.%n%n" +
                            "Листинг: %s%nГость: %s%nДаты: %s%n%n" +
                            "Зайдите в Tasklist, чтобы одобрить или отклонить.%n%nС уважением,%nAirbnb BPM",
                    title, booking.getGuestId(), dates);

            case "BOOKING_APPROVED:GUEST" -> String.format(
                    "Здравствуйте!%n%nВаша заявка одобрена.%n%n" +
                            "Листинг: %s%nДаты: %s%n%n" +
                            "Не забудьте подтвердить заезд в Tasklist.%n%nС уважением,%nAirbnb BPM",
                    title, dates);

            case "BOOKING_REJECTED:GUEST" -> String.format(
                    "Здравствуйте!%n%nВаша заявка отклонена.%n%n" +
                            "Листинг: %s%nДаты: %s%nКомментарий: %s%n%n" +
                            "С уважением,%nAirbnb BPM",
                    title, dates, orDash(booking.getOwnerComment()));

            case "BOOKING_CHECKED_IN:GUEST" -> String.format(
                    "Здравствуйте!%n%nВаш заезд подтверждён.%n%n" +
                            "Листинг: %s%nДаты: %s%n%n" +
                            "Хорошего отдыха!%n%nС уважением,%nAirbnb BPM",
                    title, dates);

            case "BOOKING_CHECKED_IN:OWNER" -> String.format(
                    "Здравствуйте!%n%nГость подтвердил заезд.%n%n" +
                            "Листинг: %s%nГость: %s%n%nС уважением,%nAirbnb BPM",
                    title, booking.getGuestId());

            case "BOOKING_CHECKED_OUT:GUEST" -> String.format(
                    "Здравствуйте!%n%nВаш выезд подтверждён.%n%n" +
                            "Листинг: %s%nДаты: %s%n%n" +
                            "Спасибо, что были с нами!%n%nС уважением,%nAirbnb BPM",
                    title, dates);

            case "BOOKING_CHECKED_OUT:OWNER" -> String.format(
                    "Здравствуйте!%n%nГость подтвердил выезд.%n%n" +
                            "Листинг: %s%nГость: %s%n%nС уважением,%nAirbnb BPM",
                    title, booking.getGuestId());

            default -> throw new ValidationException(
                    "Unknown booking notification type: " + type + ":" + recipient);
        };
    }

    private String orDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}