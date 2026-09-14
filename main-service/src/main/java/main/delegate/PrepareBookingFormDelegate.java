package main.delegate;

import main.entity.Booking;
import main.entity.Listing;
import main.entity.ListingStatus;
import main.exception.EntityNotFoundException;
import main.repository.BookingRepository;
import main.repository.ListingRepository;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("prepareBookingFormDelegate")
public class PrepareBookingFormDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareBookingFormDelegate.class);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;

    public PrepareBookingFormDelegate(BookingRepository bookingRepository,
                                      ListingRepository listingRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        List<Listing> available = listingRepository.findByStatus(ListingStatus.AVAILABLE);
        String options = available.stream()
                .map(l -> l.getId() + ": " + l.getTitle() + " (" + l.getAddress() + ")")
                .collect(Collectors.joining("; "));
        execution.setVariable("formAvailableListings", options.isEmpty() ? "—" : options);

        Long bookingId = CamundaVars.getLong(execution, "bookingId");
        if (bookingId == null) {
            log.info("PrepareBookingForm: no bookingId yet (select stage). Available={}",
                    available.size());
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
        execution.setVariable("formBookingStart", String.valueOf(booking.getBookingStart()));
        execution.setVariable("formBookingEnd", String.valueOf(booking.getBookingEnd()));
        execution.setVariable("formBookingDates",
                booking.getBookingStart() + " — " + booking.getBookingEnd());
        execution.setVariable("formOwnerComment",
                booking.getOwnerComment() != null ? booking.getOwnerComment() : "");
        execution.setVariable("formGuestComment",
                booking.getOwnerComment() != null ? booking.getOwnerComment() : "");

        log.info("PrepareBookingForm: bookingId={}, listing={}", booking.getId(), booking.getListingId());
    }
}