package main.delegate;

import main.entity.Booking;
import main.service.BookingService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("confirmBookingDelegate")
public class ConfirmBookingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ConfirmBookingDelegate.class);

    private final BookingService bookingService;

    public ConfirmBookingDelegate(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = toLong(execution.getVariable("bookingId"));
        String comment = (String) execution.getVariable("ownerComment");

        List<Booking> autoRejected = bookingService.approveBooking(bookingId, comment);
        List<Long> ids = autoRejected.stream().map(Booking::getId).toList();

        execution.setVariable("autoRejectedBookingIds", ids);
        execution.setVariable("autoRejectedCount", ids.size());

        log.info("Booking {} approved; auto-rejected {} overlapping", bookingId, ids.size());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}