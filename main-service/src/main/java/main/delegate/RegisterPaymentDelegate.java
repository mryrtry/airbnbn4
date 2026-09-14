package main.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("registerPaymentDelegate")
public class RegisterPaymentDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RegisterPaymentDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("complaintStatus", "PAID");
        execution.setVariable("windowStatus", "CLOSED");
        execution.setVariable("windowClosedAt", Instant.now().toString());
        execution.setVariable("paidAt", Instant.now().toString());
        log.info("[RESOLUTION] Guest PAID compensation for bookingId={}",
                execution.getVariable("bookingId"));
    }
}