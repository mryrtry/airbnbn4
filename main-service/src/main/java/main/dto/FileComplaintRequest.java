package main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FileComplaintRequest(
        @NotNull Long bookingId,
        @NotBlank @Size(max = 2000) String reason,
        @Size(max = 1000) String ownerComment
) {
}