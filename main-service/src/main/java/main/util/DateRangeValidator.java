package main.util;

import main.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class DateRangeValidator {

    private DateRangeValidator() {
    }

    public static void validate(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new ValidationException("Both bookingStart and bookingEnd are required");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("bookingEnd must be after bookingStart");
        }
        LocalDate today = LocalDate.now();
        if (start.isBefore(today) || end.isBefore(today)) {
            throw new ValidationException("Booking dates cannot be in the past");
        }
    }

    public static LocalDate parseIso(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException(
                    fieldName + " must be in ISO format YYYY-MM-DD, got: " + value);
        }
    }
}