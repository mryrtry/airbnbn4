package main.delegate;

import main.entity.Booking;
import main.entity.Listing;
import main.exception.EntityNotFoundException;
import main.repository.BookingRepository;
import main.repository.ListingRepository;
import main.service.BookingNotificationComposer;
import main.service.NotificationContextService;
import main.util.CamundaVars;
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
    private final NotificationContextService contextService;
    private final BookingNotificationComposer composer;

    public PrepareBookingNotificationDelegate(BookingRepository bookingRepository,
                                              ListingRepository listingRepository,
                                              NotificationContextService contextService,
                                              BookingNotificationComposer composer) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.contextService = contextService;
        this.composer = composer;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String type = CamundaVars.getString(execution, "notificationType");
        String recipient = CamundaVars.getString(execution, "notificationRecipient");

        Long bookingId = CamundaVars.getLong(execution, "bookingId");
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        Listing listing = listingRepository.findById(booking.getListingId()).orElse(null);

        String recipientUser = contextService.resolveRecipient(
                recipient, booking.getGuestId(), booking.getOwnerId());

        String subject = composer.subject(type, recipient, booking, listing);
        String body = composer.body(type, recipient, booking, listing);

        execution.setVariable("notificationRecipientEmail", contextService.resolveEmail(recipientUser));
        execution.setVariable("notificationRecipientName", recipientUser);
        execution.setVariable("notificationSubject", subject);
        execution.setVariable("notificationBody", body);

        log.info("Prepared notification type={}, recipient={}, to={}",
                type, recipient, recipientUser);
    }
}