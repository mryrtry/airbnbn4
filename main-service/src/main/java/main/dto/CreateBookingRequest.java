package main.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotNull Long listingId,
        @NotNull LocalDate bookingStart,
        @NotNull LocalDate bookingEnd
) {
}