package main.dto;

import main.entity.Listing;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingDto(
        Long id,
        String title,
        String address,
        String description,
        BigDecimal price,
        String status,
        String ownerId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ListingDto from(Listing listing) {
        return new ListingDto(
                listing.getId(),
                listing.getTitle(),
                listing.getAddress(),
                listing.getDescription(),
                listing.getPrice(),
                listing.getStatus().name(),
                listing.getOwnerId(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }
}