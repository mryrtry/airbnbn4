package main.controller;

import main.dto.BookingDto;
import main.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/my")
    @PreAuthorize("@permissionService.hasPermission('BOOKING_READ')")
    public ResponseEntity<List<BookingDto>> getMy(Authentication auth) {
        return ResponseEntity.ok(bookingService.getMyBookings(auth.getName()));
    }

    @GetMapping("/listing/{listingId}")
    @PreAuthorize("@permissionService.hasPermission('BOOKING_READ')")
    public ResponseEntity<List<BookingDto>> getByListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(bookingService.getByListing(listingId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('BOOKING_READ')")
    public ResponseEntity<BookingDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }
}