package main.delegate;

import main.entity.Booking;
import main.service.BookingLifecycleService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("confirmBookingDelegate")
public class ConfirmBookingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ConfirmBookingDelegate.class);

    private final BookingLifecycleService lifecycleService;

    public ConfirmBookingDelegate(BookingLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = CamundaVars.getLong(execution, "bookingId");
        String comment = CamundaVars.getString(execution, "ownerComment");

        List<Booking> autoRejected = lifecycleService.approve(bookingId, comment);
        List<Long> ids = autoRejected.stream().map(Booking::getId).toList();

        execution.setVariable("autoRejectedBookingIds", ids);
        execution.setVariable("autoRejectedCount", ids.size());

        log.info("Booking {} approved; auto-rejected {} overlapping", bookingId, ids.size());
    }
}