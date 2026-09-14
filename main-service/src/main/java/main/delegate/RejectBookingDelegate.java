package main.delegate;

import main.service.BookingLifecycleService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("rejectBookingDelegate")
public class RejectBookingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RejectBookingDelegate.class);

    private final BookingLifecycleService lifecycleService;

    public RejectBookingDelegate(BookingLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = CamundaVars.getLong(execution, "bookingId");
        String comment = CamundaVars.getString(execution, "ownerComment");

        lifecycleService.reject(bookingId, comment);
        log.info("Booking {} rejected", bookingId);
    }
}