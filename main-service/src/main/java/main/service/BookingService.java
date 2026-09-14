package main.service;

import main.dto.BookingDto;
import main.entity.Booking;
import main.entity.BookingStatus;
import main.entity.Listing;
import main.entity.ListingStatus;
import main.exception.EntityNotFoundException;
import main.repository.BookingRepository;
import main.repository.ListingRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    /**
     * Статусы, которые блокируют новые брони (пересечение запрещено).
     */
    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.APPROVED,
            BookingStatus.CHECKED_IN
    );

    /**
     * Все «активные» статусы — что учитывать при апруве (кого надо отклонять).
     */
    private static final List<BookingStatus> ACTIVE_FOR_APPROVE = List.of(
            BookingStatus.PENDING_OWNER,
            BookingStatus.APPROVED,
            BookingStatus.CHECKED_IN
    );

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public BookingService(BookingRepository bookingRepository,
                          ListingRepository listingRepository,
                          RuntimeService runtimeService,
                          TaskService taskService) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    // ──────────────────────── READ ────────────────────────

    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(String guestId) {
        return bookingRepository.findByGuestIdOrderByCreatedAtDesc(guestId)
                .stream().map(BookingDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getByListing(Long listingId) {
        return bookingRepository.findByListingIdOrderByCreatedAtDesc(listingId)
                .stream().map(BookingDto::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingDto getBooking(Long id) {
        return BookingDto.from(findOrThrow(id));
    }

    /**
     * Блокирует ли новая бронь на [start,end] существующие APPROVED/CHECKED_IN.
     * PENDING_OWNER НЕ блокирует.
     */
    @Transactional(readOnly = true)
    public boolean hasBlockingBooking(Long listingId, LocalDate start, LocalDate end) {
        return !bookingRepository.findOverlapping(listingId, BLOCKING_STATUSES, start, end).isEmpty();
    }

    // ──────────────────────── APPROVE ────────────────────────

    /**
     * Owner апрувит одну бронь:
     * 1. Все пересекающиеся PENDING_OWNER/APPROVED/CHECKED_IN (кроме текущей)
     * переводятся в REJECTED.
     * 2. Их userTask'и в Camunda принудительно завершаются с ownerDecision=AUTO_REJECT,
     * чтобы процессы корректно ушли в reject-ветку (уведомление + завершение).
     * 3. Текущая бронь становится APPROVED.
     * 4. Листинг становится BOOKED.
     *
     * @return список броней, которые были авто-отклонены
     */
    @Transactional
    public List<Booking> approveBooking(Long bookingId, String comment) {
        Booking current = findOrThrow(bookingId);
        Listing listing = findListingOrThrow(current.getListingId());

        List<Booking> overlapping = bookingRepository.findOverlappingExcluding(
                current.getListingId(),
                ACTIVE_FOR_APPROVE,
                current.getBookingStart(),
                current.getBookingEnd(),
                bookingId
        );

        Instant now = Instant.now();
        String autoComment = (comment != null && !comment.isBlank())
                ? comment
                : "Листинг уже забронирован на эти даты";

        List<Booking> rejected = new ArrayList<>();

        for (Booking other : overlapping) {
            // 1. Отменяем процесс в Camunda с ownerDecision=AUTO_REJECT
            terminateTaskWithAutoReject(other, autoComment);

            // 2. Меняем статус в БД
            if (other.getStatus() == BookingStatus.PENDING_OWNER
                    || other.getStatus() == BookingStatus.APPROVED
                    || other.getStatus() == BookingStatus.CHECKED_IN) {
                other.setStatus(BookingStatus.REJECTED);
                other.setOwnerComment(autoComment);
                other.setUpdatedAt(now);
                bookingRepository.save(other);
                rejected.add(other);
            }
        }

        // 3. Апрув текущей
        current.setStatus(BookingStatus.APPROVED);
        current.setUpdatedAt(now);
        bookingRepository.save(current);

        // 4. Листинг BOOKED
        listing.setStatus(ListingStatus.BOOKED);
        listing.setUpdatedAt(now);
        listingRepository.save(listing);

        log.info("Booking {} approved; {} other booking(s) auto-rejected; listing {} → BOOKED",
                bookingId, rejected.size(), listing.getId());

        return rejected;
    }

    /**
     * Принудительно завершает задачу процесса другой брони с ownerDecision=AUTO_REJECT.
     * Процесс сам пойдёт в reject-ветку, отправит уведомление и завершится.
     */
    private void terminateTaskWithAutoReject(Booking other, String comment) {
        String pid = other.getProcessInstanceId();
        if (pid == null) return;

        try {
            Task task = taskService.createTaskQuery()
                    .processInstanceId(pid)
                    .taskDefinitionKey("Task_OwnerDecide")
                    .singleResult();

            if (task != null) {
                Map<String, Object> vars = new HashMap<>();
                vars.put("ownerDecision", "AUTO_REJECT");
                vars.put("ownerComment", comment);
                taskService.complete(task.getId(), vars);
                log.info("Auto-completed Task_OwnerDecide for booking {}", other.getId());
            } else {
                // Процесс уже не на userTask — просто убираем инстанс
                runtimeService.deleteProcessInstance(pid, "Auto-rejected due to overlapping approved booking");
                log.info("Deleted process instance {} for booking {}", pid, other.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to auto-reject task for booking {} (pid={})", other.getId(), pid, e);
        }
    }

    // ──────────────────────── REJECT ────────────────────────

    @Transactional
    public void rejectBooking(Long bookingId, String comment) {
        Booking booking = findOrThrow(bookingId);
        booking.setStatus(BookingStatus.REJECTED);
        if (comment != null && !comment.isBlank()) {
            booking.setOwnerComment(comment);
        }
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);
        log.info("Booking {} rejected", bookingId);
    }

    // ──────────────────────── CHECK-IN ────────────────────────

    @Transactional
    public void checkIn(Long bookingId) {
        Booking booking = findOrThrow(bookingId);
        Listing listing = findListingOrThrow(booking.getListingId());

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        listing.setStatus(ListingStatus.LIVING);
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        log.info("Booking {} checked-in; listing {} → LIVING", bookingId, listing.getId());
    }

    // ──────────────────────── CHECK-OUT ────────────────────────

    @Transactional
    public void checkOut(Long bookingId) {
        Booking booking = findOrThrow(bookingId);
        Listing listing = findListingOrThrow(booking.getListingId());

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        // После выезда листинг снова доступен
        listing.setStatus(ListingStatus.AVAILABLE);
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        log.info("Booking {} checked-out; listing {} → AVAILABLE", bookingId, listing.getId());
    }

    // ──────────────────────── helpers ────────────────────────

    private Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + id));
    }

    private Listing findListingOrThrow(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + id));
    }
}