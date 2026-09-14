package main.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("closeResolutionWindowExpiredDelegate")
public class CloseResolutionWindowExpiredDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CloseResolutionWindowExpiredDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("windowStatus", "EXPIRED");
        execution.setVariable("windowClosedAt", Instant.now().toString());

        log.info("[RESOLUTION] Window expired (no complaint filed) for bookingId={}",
                execution.getVariable("bookingId"));
    }
}