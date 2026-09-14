package main.dto;

import main.entity.Booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingDto(
        Long id,
        Long listingId,
        String guestId,
        String ownerId,
        String status,
        LocalDate bookingStart,
        LocalDate bookingEnd,
        String ownerComment,
        Long bitrixDealId,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookingDto from(Booking b) {
        return new BookingDto(
                b.getId(),
                b.getListingId(),
                b.getGuestId(),
                b.getOwnerId(),
                b.getStatus().name(),
                b.getBookingStart(),
                b.getBookingEnd(),
                b.getOwnerComment(),
                b.getBitrixDealId(),
                b.getTotalPrice(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
