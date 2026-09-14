package main.service;

import main.entity.Booking;
import main.entity.BookingStatus;
import main.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingAvailabilityService {

    public static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.APPROVED,
            BookingStatus.CHECKED_IN
    );

    public static final List<BookingStatus> ACTIVE_FOR_APPROVE = List.of(
            BookingStatus.PENDING_OWNER,
            BookingStatus.APPROVED,
            BookingStatus.CHECKED_IN
    );

    private final BookingRepository bookingRepository;

    public BookingAvailabilityService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasBlockingBooking(Long listingId, LocalDate start, LocalDate end) {
        return !bookingRepository
                .findOverlapping(listingId, BLOCKING_STATUSES, start, end)
                .isEmpty();
    }

    @Transactional(readOnly = true)
    public List<Booking> findOverlappingForApprove(Long listingId,
                                                   LocalDate start,
                                                   LocalDate end,
                                                   Long excludeId) {
        return bookingRepository.findOverlappingExcluding(
                listingId, ACTIVE_FOR_APPROVE, start, end, excludeId);
    }
}