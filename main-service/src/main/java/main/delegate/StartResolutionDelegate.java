package main.delegate;

import main.entity.Booking;
import main.exception.EntityNotFoundException;
import main.repository.BookingRepository;
import main.util.CamundaVars;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("startResolutionDelegate")
public class StartResolutionDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(StartResolutionDelegate.class);

    private static final String PROCESS_KEY = "resolution-lifecycle";
    private static final String BUSINESS_KEY_PREFIX = "resolution-booking-";

    private final RuntimeService runtimeService;
    private final BookingRepository bookingRepository;

    public StartResolutionDelegate(RuntimeService runtimeService,
                                   BookingRepository bookingRepository) {
        this.runtimeService = runtimeService;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = CamundaVars.getLong(execution, "bookingId");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        String businessKey = BUSINESS_KEY_PREFIX + bookingId;

        ProcessInstance existing = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (existing != null) {
            log.info("[RESOLUTION] Process already running for bookingId={}, pid={}",
                    bookingId, existing.getId());
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("bookingId", bookingId);
        vars.put("ownerId", booking.getOwnerId());
        vars.put("guestId", booking.getGuestId());
        vars.put("bookingStart", booking.getBookingStart().toString());
        vars.put("bookingEnd", booking.getBookingEnd().toString());
        vars.put("initiatorUserId", booking.getOwnerId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, businessKey, vars);
        execution.setVariable("resolutionProcessInstanceId", pi.getId());

        log.info("[RESOLUTION] Started resolution-lifecycle for bookingId={}, pid={}",
                bookingId, pi.getId());
    }
}