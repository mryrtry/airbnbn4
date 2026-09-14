package main.delegate;

import main.entity.Booking;
import main.entity.Listing;
import main.exception.EntityNotFoundException;
import main.repository.BookingRepository;
import main.repository.ListingRepository;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("prepareResolutionFormDelegate")
public class PrepareResolutionFormDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareResolutionFormDelegate.class);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;

    public PrepareResolutionFormDelegate(BookingRepository bookingRepository,
                                         ListingRepository listingRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = CamundaVars.getLong(execution, "bookingId");
        if (bookingId == null) {
            log.warn("PrepareResolutionForm: bookingId is null");
            return;
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        Listing listing = listingRepository.findById(booking.getListingId()).orElse(null);

        execution.setVariable("formBookingId", booking.getId());
        execution.setVariable("formListingTitle", listing != null ? listing.getTitle() : "—");
        execution.setVariable("formListingAddress", listing != null ? listing.getAddress() : "—");
        execution.setVariable("formGuestId", booking.getGuestId());
        execution.setVariable("formOwnerId", booking.getOwnerId());
        execution.setVariable("formBookingDates",
                booking.getBookingStart() + " — " + booking.getBookingEnd());

        execution.setVariable("formReason", stringOrEmpty(execution.getVariable("reason")));
        execution.setVariable("formOwnerComment", stringOrEmpty(execution.getVariable("ownerComment")));
        execution.setVariable("formAdminComment", stringOrEmpty(execution.getVariable("adminComment")));
    }

    private String stringOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}