package main.service;

import main.dto.BookingDto;
import main.entity.Booking;
import main.exception.EntityNotFoundException;
import main.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingQueryService {

    private final BookingRepository bookingRepository;

    public BookingQueryService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(String guestId) {
        return bookingRepository.findByGuestIdOrderByCreatedAtDesc(guestId)
                .stream()
                .map(BookingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getByListing(Long listingId) {
        return bookingRepository.findByListingIdOrderByCreatedAtDesc(listingId)
                .stream()
                .map(BookingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingDto getBooking(Long id) {
        return BookingDto.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + id));
    }
}