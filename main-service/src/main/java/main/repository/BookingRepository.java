package main.repository;

import main.entity.Booking;
import main.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByGuestIdOrderByCreatedAtDesc(String guestId);

    List<Booking> findByListingIdOrderByCreatedAtDesc(Long listingId);

    Optional<Booking> findByProcessInstanceId(String processInstanceId);

    /**
     * Пересекающиеся брони на листинг с указанными статусами.
     * Используем строгое неравенство — back-to-back разрешён.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.listingId = :listingId
          AND b.status IN :statuses
          AND b.bookingStart < :end
          AND :start < b.bookingEnd
    """)
    List<Booking> findOverlapping(
            @Param("listingId") Long listingId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * То же, но исключая бронь с указанным id — используется при апруве,
     * чтобы не отклонять саму себя.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.listingId = :listingId
          AND b.status IN :statuses
          AND b.id <> :excludeId
          AND b.bookingStart < :end
          AND :start < b.bookingEnd
    """)
    List<Booking> findOverlappingExcluding(
            @Param("listingId") Long listingId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("excludeId") Long excludeId);
}