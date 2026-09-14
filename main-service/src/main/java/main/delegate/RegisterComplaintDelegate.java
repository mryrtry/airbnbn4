package main.delegate;

import main.exception.ValidationException;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("registerComplaintDelegate")
public class RegisterComplaintDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RegisterComplaintDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String reason = CamundaVars.getString(execution, "reason");
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Complaint reason is required");
        }
        execution.setVariable("complaintStatus", "PENDING");
        execution.setVariable("complaintFiledAt", Instant.now().toString());
        log.info("[RESOLUTION] Complaint filed for bookingId={}, reason='{}'",
                CamundaVars.getString(execution, "bookingId"), reason);
    }
}