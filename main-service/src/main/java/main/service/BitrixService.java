package main.service;

import main.entity.Booking;
import main.entity.BookingStatus;
import main.entity.Listing;
import main.integration.bitrix.BitrixClient;
import main.integration.bitrix.BitrixDealRequest;
import main.integration.bitrix.BitrixDealUpdate;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.User;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

@Service
public class BitrixService {

    private final BitrixClient bitrixClient;
    private final IdentityService identityService;

    public BitrixService(BitrixClient bitrixClient, IdentityService identityService) {
        this.bitrixClient = bitrixClient;
        this.identityService = identityService;
    }

    public long createBookingDeal(Booking booking, Listing listing) throws Exception {
        BitrixDealRequest request = new BitrixDealRequest(
                "Airbnb booking #" + booking.getId() + " — " + listing.getTitle(),
                stageFor(booking.getStatus()),
                buildComments(booking, listing),
                booking.getTotalPrice(),
                "RUB"
        );
        return bitrixClient.createDeal(request);
    }

    public void updateBookingDeal(Booking booking) throws Exception {
        if (booking.getBitrixDealId() == null) {
            return;
        }
        bitrixClient.updateDeal(
                booking.getBitrixDealId(),
                new BitrixDealUpdate(stageFor(booking.getStatus()), null)
        );
    }

    private String stageFor(BookingStatus status) {
        return switch (status) {
            case PENDING_OWNER -> "NEW";
            case APPROVED -> "PREPARATION";
            case REJECTED, CANCELLED -> "LOSE";
            case CHECKED_IN -> "EXECUTING";
            case CHECKED_OUT -> "WON";
        };
    }

    private String buildComments(Booking booking, Listing listing) {
        long nights = ChronoUnit.DAYS.between(booking.getBookingStart(), booking.getBookingEnd());
        User guest = identityService.createUserQuery().userId(booking.getGuestId()).singleResult();
        String guestEmail = guest != null && guest.getEmail() != null ? guest.getEmail() : "—";

        return "Airbnb listing #" + listing.getId()
                + "\nGuest: " + booking.getGuestId() + " (" + guestEmail + ")"
                + "\nCheck-in: " + booking.getBookingStart()
                + "\nCheck-out: " + booking.getBookingEnd()
                + "\nNights: " + nights
                + "\nPrice per night: " + listing.getPrice() + " RUB"
                + "\nTotal price: " + booking.getTotalPrice() + " RUB";
    }
}
