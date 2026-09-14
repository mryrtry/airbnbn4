package main.delegate;

import main.entity.Booking;
import main.entity.Listing;
import main.entity.ListingStatus;
import main.exception.EntityNotFoundException;
import main.repository.BookingRepository;
import main.repository.ListingRepository;
import main.util.CamundaVars;
import main.util.UserNames;
import org.camunda.bpm.engine.IdentityService;
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
    private final IdentityService identityService;

    public PrepareBookingFormDelegate(BookingRepository bookingRepository,
                                      ListingRepository listingRepository,
                                      IdentityService identityService) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.identityService = identityService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        List<Listing> available = listingRepository.findByStatus(ListingStatus.AVAILABLE);
        String options = available.stream()
                .map(this::formatListing)
                .collect(Collectors.joining("\n\n"));
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

    private String formatListing(Listing listing) {
        return "ID: " + listing.getId()
                + "\nНазвание: " + listing.getTitle()
                + "\nАдрес: " + listing.getAddress()
                + "\nОписание: " + valueOrDash(listing.getDescription())
                + "\nВладелец: " + UserNames.resolve(identityService, listing.getOwnerId())
                + "\nЦена за ночь: " + listing.getPrice().toPlainString();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
