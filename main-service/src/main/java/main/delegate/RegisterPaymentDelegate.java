package main.delegate;

import main.util.CamundaVars;
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
        Instant now = Instant.now();
        execution.setVariable("complaintStatus", "PAID");
        execution.setVariable("windowStatus", "CLOSED");
        execution.setVariable("windowClosedAt", now.toString());
        execution.setVariable("paidAt", now.toString());
        log.info("[RESOLUTION] Guest PAID compensation for bookingId={}",
                CamundaVars.getString(execution, "bookingId"));
    }
}