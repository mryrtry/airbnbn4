package main.service;

import main.dto.ProcessStartResult;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class BookingProcessService {

    private static final String PROCESS_KEY = "booking-lifecycle";

    private final RuntimeService runtimeService;

    public BookingProcessService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public ProcessStartResult startCreate(String guestId, Long listingId,
                                          LocalDate bookingStart, LocalDate bookingEnd) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("initiatorUserId", guestId);
        vars.put("guestId", guestId);

        if (listingId != null) vars.put("listingId", listingId);
        if (bookingStart != null) vars.put("bookingStart", bookingStart);
        if (bookingEnd != null) vars.put("bookingEnd", bookingEnd);

        String businessKey = listingId != null ? "booking-listing-" + listingId : null;

        ProcessInstance instance = businessKey != null
                ? runtimeService.startProcessInstanceByKey(PROCESS_KEY, businessKey, vars)
                : runtimeService.startProcessInstanceByKey(PROCESS_KEY, vars);

        return new ProcessStartResult(instance.getId(), "CREATE_BOOKING", listingId, "started");
    }
}