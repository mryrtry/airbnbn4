package main.delegate;

import main.entity.Booking;
import main.entity.BookingStatus;
import main.entity.Listing;
import main.repository.BookingRepository;
import main.repository.ListingRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Component("createBookingDelegate")
public class CreateBookingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreateBookingDelegate.class);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;

    public CreateBookingDelegate(BookingRepository bookingRepository,
                                 ListingRepository listingRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        if (bookingRepository.findByProcessInstanceId(processInstanceId).isPresent()) {
            log.warn("Booking already exists for process {}", processInstanceId);
            execution.setVariable("bookingId",
                    bookingRepository.findByProcessInstanceId(processInstanceId).get().getId());
            return;
        }

        Long listingId = toLong(execution.getVariable("listingId"));
        String guestId = (String) execution.getVariable("initiatorUserId");

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalStateException("Listing not found: " + listingId));

        LocalDate start = toLocalDate(execution.getVariable("bookingStart"));
        LocalDate end = toLocalDate(execution.getVariable("bookingEnd"));

        Booking booking = new Booking();
        booking.setListingId(listingId);
        booking.setGuestId(guestId);
        booking.setOwnerId(listing.getOwnerId());
        booking.setStatus(BookingStatus.PENDING_OWNER);
        booking.setBookingStart(start);
        booking.setBookingEnd(end);
        booking.setProcessInstanceId(processInstanceId);
        booking.setCreatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        execution.setVariable("bookingId", saved.getId());
        execution.setVariable("ownerId", saved.getOwnerId());
        execution.setVariable("guestId", saved.getGuestId());

        log.info("Booking created: id={}, listing={}, guest={}", saved.getId(), listingId, guestId);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        return LocalDate.parse(value.toString());
    }
}