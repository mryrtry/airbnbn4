package main.delegate;

import main.entity.Booking;
import main.entity.Listing;
import main.repository.BookingRepository;
import main.repository.ListingRepository;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareBookingNotificationDelegate")
public class PrepareBookingNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareBookingNotificationDelegate.class);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final IdentityService identityService;

    public PrepareBookingNotificationDelegate(BookingRepository bookingRepository,
                                              ListingRepository listingRepository,
                                              IdentityService identityService) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.identityService = identityService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String type = (String) execution.getVariable("notificationType");
        String recipient = (String) execution.getVariable("notificationRecipient"); // GUEST | OWNER

        // Фолбэк для старых инстансов, где notificationType не задан
        if (type == null) {
            type = switch (execution.getCurrentActivityId()) {
                case "Task_PrepareNotifyOwner"          -> "BOOKING_CREATED";
                case "Task_PrepareNotifyRejected"       -> "BOOKING_REJECTED";
                case "Task_PrepareNotifyApproved"       -> "BOOKING_APPROVED";
                case "Task_PrepareNotifyCheckInGuest",
                     "Task_PrepareNotifyCheckInOwner"   -> "BOOKING_CHECKED_IN";
                case "Task_PrepareNotifyCheckOutGuest",
                     "Task_PrepareNotifyCheckOutOwner"  -> "BOOKING_CHECKED_OUT";
                default -> null;
            };
        }
        if (type == null) {
            log.warn("notificationType not resolved for activity {}", execution.getCurrentActivityId());
            return;
        }

        // Фолбэк для notificationRecipient (старые процессы)
        if (recipient == null) {
            recipient = switch (type) {
                case "BOOKING_CREATED"      -> "OWNER";
                case "BOOKING_REJECTED",
                     "BOOKING_APPROVED"     -> "GUEST";
                case "BOOKING_CHECKED_IN",
                     "BOOKING_CHECKED_OUT"  -> "OWNER"; // по умолчанию — owner
                default -> null;
            };
        }

        Long bookingId = toLong(execution.getVariable("bookingId"));
        Booking booking = bookingId == null ? null : bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            log.warn("Booking {} not found for notification type={}", bookingId, type);
            return;
        }
        Listing listing = listingRepository.findById(booking.getListingId()).orElse(null);

        String recipientUser = "GUEST".equals(recipient) ? booking.getGuestId() : booking.getOwnerId();
        String recipientEmail = getEmail(recipientUser);
        String subject = null;
        String body = null;

        String listingTitle = listing != null ? listing.getTitle() : "?";
        String dates = booking.getBookingStart() + " — " + booking.getBookingEnd();

        switch (type + ":" + recipient) {
            case "BOOKING_CREATED:OWNER" -> {
                subject = "Новая заявка на бронирование";
                body = String.format(
                        "Здравствуйте!%n%nПоступила новая заявка на бронирование.%n%n" +
                                "Листинг: %s%nГость: %s%nДаты: %s%n%n" +
                                "Зайдите в Tasklist, чтобы одобрить или отклонить.%n%nС уважением,%nAirbnb BPM",
                        listingTitle, booking.getGuestId(), dates);
            }
            case "BOOKING_APPROVED:GUEST" -> {
                subject = "Бронь одобрена";
                body = String.format(
                        "Здравствуйте!%n%nВаша заявка одобрена.%n%n" +
                                "Листинг: %s%nДаты: %s%n%n" +
                                "Не забудьте подтвердить заезд в Tasklist.%n%nС уважением,%nAirbnb BPM",
                        listingTitle, dates);
            }
            case "BOOKING_REJECTED:GUEST" -> {
                subject = "Бронь отклонена";
                body = String.format(
                        "Здравствуйте!%n%nВаша заявка отклонена.%n%n" +
                                "Листинг: %s%nДаты: %s%nКомментарий: %s%n%n" +
                                "С уважением,%nAirbnb BPM",
                        listingTitle, dates,
                        booking.getOwnerComment() != null ? booking.getOwnerComment() : "—");
            }
            case "BOOKING_CHECKED_IN:GUEST" -> {
                subject = "Заезд подтверждён";
                body = String.format(
                        "Здравствуйте!%n%nВаш заезд подтверждён.%n%n" +
                                "Листинг: %s%nДаты: %s%n%n" +
                                "Хорошего отдыха!%n%nС уважением,%nAirbnb BPM",
                        listingTitle, dates);
            }
            case "BOOKING_CHECKED_IN:OWNER" -> {
                subject = "Гость заехал";
                body = String.format(
                        "Здравствуйте!%n%nГость подтвердил заезд.%n%n" +
                                "Листинг: %s%nГость: %s%n%nС уважением,%nAirbnb BPM",
                        listingTitle, booking.getGuestId());
            }
            case "BOOKING_CHECKED_OUT:GUEST" -> {
                subject = "Выезд подтверждён";
                body = String.format(
                        "Здравствуйте!%n%nВаш выезд подтверждён.%n%n" +
                                "Листинг: %s%nДаты: %s%n%n" +
                                "Спасибо, что были с нами!%n%nС уважением,%nAirbnb BPM",
                        listingTitle, dates);
            }
            case "BOOKING_CHECKED_OUT:OWNER" -> {
                subject = "Гость выехал";
                body = String.format(
                        "Здравствуйте!%n%nГость подтвердил выезд.%n%n" +
                                "Листинг: %s%nГость: %s%n%nС уважением,%nAirbnb BPM",
                        listingTitle, booking.getGuestId());
            }
            default -> log.warn("Unknown notification combination type={}, recipient={}", type, recipient);
        }

        if (subject == null) return;

        execution.setVariable("notificationRecipientEmail", recipientEmail);
        execution.setVariable("notificationRecipientName", recipientUser);
        execution.setVariable("notificationSubject", subject);
        execution.setVariable("notificationBody", body);

        log.info("Prepared notification type={}, recipient={}, to={}", type, recipient, recipientEmail);
    }

    private String getEmail(String username) {
        if (username == null) return null;
        var user = identityService.createUserQuery().userId(username).singleResult();
        return user != null ? user.getEmail() : null;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}