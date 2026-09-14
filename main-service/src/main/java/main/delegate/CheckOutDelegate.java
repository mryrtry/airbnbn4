package main.delegate;

import main.service.BookingLifecycleService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkOutDelegate")
public class CheckOutDelegate implements JavaDelegate {

    private final BookingLifecycleService lifecycleService;

    public CheckOutDelegate(BookingLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = CamundaVars.getLong(execution, "bookingId");
        lifecycleService.checkOut(bookingId);
    }
}