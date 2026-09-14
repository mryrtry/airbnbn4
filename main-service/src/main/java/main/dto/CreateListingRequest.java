package main.dto;

import java.math.BigDecimal;

public record CreateListingRequest(
        String title,
        String address,
        String description,
        BigDecimal price
) {
}