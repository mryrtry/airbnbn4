package main.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("approveComplaintDelegate")
public class ApproveComplaintDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ApproveComplaintDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("complaintStatus", "APPROVED");
        log.info("[RESOLUTION] Complaint APPROVED for bookingId={}, adminComment={}",
                execution.getVariable("bookingId"), execution.getVariable("adminComment"));
    }
}