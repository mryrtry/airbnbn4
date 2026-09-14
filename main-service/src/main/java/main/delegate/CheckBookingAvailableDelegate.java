package main.delegate;

import main.entity.Listing;
import main.entity.ListingStatus;
import main.repository.ListingRepository;
import main.service.BookingService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("checkBookingAvailableDelegate")
public class CheckBookingAvailableDelegate implements JavaDelegate {

    public static final String ERROR_NOT_FOUND = "NOT_FOUND";
    public static final String ERROR_NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String ERROR_OWN_LISTING = "OWN_LISTING";
    public static final String ERROR_HAS_ACTIVE = "HAS_ACTIVE";
    public static final String ERROR_NO_DATES = "NO_DATES";
    public static final String ERROR_INVALID_DATES = "INVALID_DATES";
    public static final String ERROR_DATE_IN_PAST = "DATE_IN_PAST";

    private static final Logger log = LoggerFactory.getLogger(CheckBookingAvailableDelegate.class);

    private final ListingRepository listingRepository;
    private final BookingService bookingService;

    public CheckBookingAvailableDelegate(ListingRepository listingRepository,
                                         BookingService bookingService) {
        this.listingRepository = listingRepository;
        this.bookingService = bookingService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.removeVariable("checkError");

        Long listingId = toLong(execution.getVariable("listingId"));
        String guestId = (String) execution.getVariable("initiatorUserId");
        LocalDate start = toLocalDate(execution.getVariable("bookingStart"));
        LocalDate end = toLocalDate(execution.getVariable("bookingEnd"));
        execution.setVariable("guestId", guestId);

        if (listingId == null) {
            execution.setVariable("checkError", ERROR_NOT_FOUND);
            return;
        }
        if (start == null || end == null) {
            execution.setVariable("checkError", ERROR_NO_DATES);
            return;
        }
        if (!end.isAfter(start)) {
            execution.setVariable("checkError", ERROR_INVALID_DATES);
            return;
        }
        LocalDate today = LocalDate.now();
        if (start.isBefore(today) || end.isBefore(today)) {
            execution.setVariable("checkError", ERROR_DATE_IN_PAST);
            return;
        }

        Listing listing = listingRepository.findById(listingId).orElse(null);
        if (listing == null || listing.getStatus() == ListingStatus.DELETED) {
            execution.setVariable("checkError", ERROR_NOT_FOUND);
            return;
        }
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            execution.setVariable("checkError", ERROR_NOT_AVAILABLE);
            return;
        }
        if (listing.getOwnerId().equals(guestId)) {
            execution.setVariable("checkError", ERROR_OWN_LISTING);
            return;
        }
        if (bookingService.hasBlockingBooking(listingId, start, end)) {
            execution.setVariable("checkError", ERROR_HAS_ACTIVE);
            return;
        }

        execution.setVariable("ownerId", listing.getOwnerId());
        log.info("Listing {} available for {} ({}—{})", listingId, guestId, start, end);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        try { return LocalDate.parse(v.toString()); } catch (Exception e) { return null; }
    }
}