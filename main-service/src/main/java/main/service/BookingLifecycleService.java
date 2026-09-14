package main.service;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(BookingLifecycleService.class);

    private static final String TASK_OWNER_DECIDE = "Task_OwnerDecide";
    private static final String VAR_OWNER_DECISION = "ownerDecision";
    private static final String VAR_OWNER_COMMENT = "ownerComment";
    private static final String DECISION_AUTO_REJECT = "AUTO_REJECT";
    private static final String DEFAULT_AUTO_REJECT_COMMENT =
            "Listing already booked for these dates";

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final BookingAvailabilityService availabilityService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public BookingLifecycleService(BookingRepository bookingRepository,
                                   ListingRepository listingRepository,
                                   BookingAvailabilityService availabilityService,
                                   RuntimeService runtimeService,
                                   TaskService taskService) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.availabilityService = availabilityService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Transactional
    public List<Booking> approve(Long bookingId, String comment) {
        Booking current = findOrThrow(bookingId);
        Listing listing = findListingOrThrow(current.getListingId());

        List<Booking> overlapping = availabilityService.findOverlappingForApprove(
                current.getListingId(),
                current.getBookingStart(),
                current.getBookingEnd(),
                bookingId);

        String autoComment = (comment != null && !comment.isBlank())
                ? comment
                : DEFAULT_AUTO_REJECT_COMMENT;

        List<Booking> rejected = new ArrayList<>();
        Instant now = Instant.now();

        for (Booking other : overlapping) {
            terminateOwnerTask(other, autoComment);
            other.setStatus(BookingStatus.REJECTED);
            other.setOwnerComment(autoComment);
            other.setUpdatedAt(now);
            bookingRepository.save(other);
            rejected.add(other);
        }

        current.setStatus(BookingStatus.APPROVED);
        current.setUpdatedAt(now);
        bookingRepository.save(current);

        listing.setStatus(ListingStatus.BOOKED);
        listing.setUpdatedAt(now);
        listingRepository.save(listing);

        log.info("Booking {} approved; {} auto-rejected; listing {} → BOOKED",
                bookingId, rejected.size(), listing.getId());
        return rejected;
    }

    private void terminateOwnerTask(Booking booking, String comment) {
        String pid = booking.getProcessInstanceId();
        if (pid == null) {
            return;
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(pid)
                .taskDefinitionKey(TASK_OWNER_DECIDE)
                .singleResult();

        if (task != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.put(VAR_OWNER_DECISION, DECISION_AUTO_REJECT);
            vars.put(VAR_OWNER_COMMENT, comment);
            taskService.complete(task.getId(), vars);
            log.info("Auto-completed {} for booking {}", TASK_OWNER_DECIDE, booking.getId());
            return;
        }

        runtimeService.deleteProcessInstance(pid,
                "Auto-rejected due to overlapping approved booking");
        log.info("Deleted process instance {} for booking {}", pid, booking.getId());
    }

    @Transactional
    public void reject(Long bookingId, String comment) {
        Booking booking = findOrThrow(bookingId);
        booking.setStatus(BookingStatus.REJECTED);
        if (comment != null && !comment.isBlank()) {
            booking.setOwnerComment(comment);
        }
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);
        log.info("Booking {} rejected", bookingId);
    }

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

    @Transactional
    public void checkOut(Long bookingId) {
        Booking booking = findOrThrow(bookingId);
        Listing listing = findListingOrThrow(booking.getListingId());

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        listing.setStatus(ListingStatus.AVAILABLE);
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        log.info("Booking {} checked-out; listing {} → AVAILABLE", bookingId, listing.getId());
    }

    private Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + id));
    }

    private Listing findListingOrThrow(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + id));
    }
}