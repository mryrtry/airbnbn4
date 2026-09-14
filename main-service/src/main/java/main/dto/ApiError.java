package main.dto;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String error,
        String message,
        Map<String, String> fields,
        Instant timestamp
) {
    public static ApiError of(String error, String message) {
        return new ApiError(error, message, null, Instant.now());
    }

    public static ApiError withFields(String error, String message, Map<String, String> fields) {
        return new ApiError(error, message, fields, Instant.now());
    }
}