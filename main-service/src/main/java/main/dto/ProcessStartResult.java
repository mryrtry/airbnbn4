package main.dto;

public record ProcessStartResult(
        String processInstanceId,
        String operation,
        Long listingId,
        String status
) {
}