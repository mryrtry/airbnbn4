package main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateListingRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String address,
        @Size(max = 2000) String description,
        @NotNull @PositiveOrZero BigDecimal price
) {
}