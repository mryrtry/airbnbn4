package main.delegate;

import main.entity.Booking;
import main.repository.BookingRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("startResolutionDelegate")
public class StartResolutionDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(StartResolutionDelegate.class);

    private final RuntimeService runtimeService;
    private final BookingRepository bookingRepository;

    public StartResolutionDelegate(RuntimeService runtimeService,
                                   BookingRepository bookingRepository) {
        this.runtimeService = runtimeService;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = toLong(execution.getVariable("bookingId"));
        if (bookingId == null) throw new IllegalStateException("bookingId is null");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found: " + bookingId));

        Map<String, Object> vars = new HashMap<>();
        vars.put("bookingId", bookingId);
        vars.put("ownerId", booking.getOwnerId());
        vars.put("guestId", booking.getGuestId());
        vars.put("bookingStart", booking.getBookingStart().toString());
        vars.put("bookingEnd", booking.getBookingEnd().toString());
        vars.put("initiatorUserId", booking.getOwnerId());

        String businessKey = "resolution-booking-" + bookingId;

        // не стартуем дубль, если уже есть активный процесс
        var existing = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("resolution-lifecycle")
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (existing != null) {
            log.info("[RESOLUTION] Process already running for bookingId={}, pid={}",
                    bookingId, existing.getId());
            return;
        }

        var pi = runtimeService.startProcessInstanceByKey("resolution-lifecycle", businessKey, vars);
        execution.setVariable("resolutionProcessInstanceId", pi.getId());

        log.info("[RESOLUTION] Started resolution-lifecycle for bookingId={}, pid={}",
                bookingId, pi.getId());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}