package main.delegate;

import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("rejectComplaintDelegate")
public class RejectComplaintDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RejectComplaintDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("complaintStatus", "REJECTED");
        execution.setVariable("windowStatus", "CLOSED");
        execution.setVariable("windowClosedAt", Instant.now().toString());
        log.info("[RESOLUTION] Complaint REJECTED for bookingId={}, adminComment={}",
                CamundaVars.getString(execution, "bookingId"),
                CamundaVars.getString(execution, "adminComment"));
    }
}