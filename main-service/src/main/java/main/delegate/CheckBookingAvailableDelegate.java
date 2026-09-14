package main.delegate;

import main.entity.Listing;
import main.entity.ListingStatus;
import main.exception.ValidationException;
import main.repository.ListingRepository;
import main.service.BookingAvailabilityService;
import main.util.CamundaVars;
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
    private final BookingAvailabilityService availabilityService;

    public CheckBookingAvailableDelegate(ListingRepository listingRepository,
                                         BookingAvailabilityService availabilityService) {
        this.listingRepository = listingRepository;
        this.availabilityService = availabilityService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.removeVariable("checkError");

        Long listingId = CamundaVars.getLong(execution, "listingId");
        String guestId = CamundaVars.getString(execution, "initiatorUserId");
        LocalDate start = CamundaVars.getLocalDate(execution, "bookingStart");
        LocalDate end = CamundaVars.getLocalDate(execution, "bookingEnd");

        execution.setVariable("guestId", guestId);

        String error = validate(listingId, guestId, start, end);
        if (error != null) {
            execution.setVariable("checkError", error);
            return;
        }

        Listing listing = listingRepository.findById(listingId).orElse(null);

        error = checkListing(listing, guestId, listingId, start, end);
        if (error != null) {
            execution.setVariable("checkError", error);
            return;
        }

        execution.setVariable("ownerId", listing.getOwnerId());
        log.info("Listing {} available for {} ({}—{})", listingId, guestId, start, end);
    }

    private String validate(Long listingId, String guestId, LocalDate start, LocalDate end) {
        if (listingId == null) {
            return ERROR_NOT_FOUND;
        }
        if (guestId == null || guestId.isBlank()) {
            throw new ValidationException("initiatorUserId is required");
        }
        if (start == null || end == null) {
            return ERROR_NO_DATES;
        }
        if (!end.isAfter(start)) {
            return ERROR_INVALID_DATES;
        }
        LocalDate today = LocalDate.now();
        if (start.isBefore(today) || end.isBefore(today)) {
            return ERROR_DATE_IN_PAST;
        }
        return null;
    }

    private String checkListing(Listing listing, String guestId, Long listingId,
                                LocalDate start, LocalDate end) {
        if (listing == null || listing.getStatus() == ListingStatus.DELETED) {
            return ERROR_NOT_FOUND;
        }
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            return ERROR_NOT_AVAILABLE;
        }
        if (listing.getOwnerId().equals(guestId)) {
            return ERROR_OWN_LISTING;
        }
        if (availabilityService.hasBlockingBooking(listingId, start, end)) {
            return ERROR_HAS_ACTIVE;
        }
        return null;
    }
}