package main.delegate;

import main.service.BookingService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkOutDelegate")
public class CheckOutDelegate implements JavaDelegate {

    private final BookingService bookingService;

    public CheckOutDelegate(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long bookingId = toLong(execution.getVariable("bookingId"));
        if (bookingId == null) throw new IllegalStateException("bookingId is null");
        bookingService.checkOut(bookingId);
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