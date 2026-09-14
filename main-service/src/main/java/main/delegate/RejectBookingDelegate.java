package main.delegate;

import main.service.BookingService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("rejectBookingDelegate")
public class RejectBookingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RejectBookingDelegate.class);

    private final BookingService bookingService;

    public RejectBookingDelegate(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = toLong(execution.getVariable("bookingId"));
        String comment = (String) execution.getVariable("ownerComment");

        bookingService.rejectBooking(bookingId, comment);
        log.info("Booking {} rejected", bookingId);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}