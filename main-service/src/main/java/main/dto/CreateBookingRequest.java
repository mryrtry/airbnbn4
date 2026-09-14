package main.dto;

import java.time.LocalDate;

public record CreateBookingRequest(
        Long listingId,
        LocalDate bookingStart,
        LocalDate bookingEnd
) {
}