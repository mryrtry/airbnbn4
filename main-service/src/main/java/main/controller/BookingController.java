package main.controller;

import main.dto.BookingDto;
import main.service.BookingQueryService;
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

    private final BookingQueryService bookingQueryService;

    public BookingController(BookingQueryService bookingQueryService) {
        this.bookingQueryService = bookingQueryService;
    }

    @GetMapping("/my")
    @PreAuthorize("@permissionService.hasPermission('BOOKING_READ')")
    public ResponseEntity<List<BookingDto>> getMy(Authentication auth) {
        return ResponseEntity.ok(bookingQueryService.getMyBookings(auth.getName()));
    }

    @GetMapping("/listing/{listingId}")
    @PreAuthorize("@permissionService.hasPermission('BOOKING_READ')")
    public ResponseEntity<List<BookingDto>> getByListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(bookingQueryService.getByListing(listingId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('BOOKING_READ')")
    public ResponseEntity<BookingDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(bookingQueryService.getBooking(id));
    }
}