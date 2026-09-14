package main.delegate;

import main.service.UserBanService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("banUserDelegate")
public class BanUserDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(BanUserDelegate.class);

    private final UserBanService userBanService;

    public BanUserDelegate(UserBanService userBanService) {
        this.userBanService = userBanService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String guestId = CamundaVars.getString(execution, "guestId");
        userBanService.ban(guestId);

        execution.setVariable("complaintStatus", "DECLINED");
        execution.setVariable("windowStatus", "CLOSED");
        execution.setVariable("windowClosedAt", java.time.Instant.now().toString());
        execution.setVariable("userBanned", true);

        log.warn("[RESOLUTION] User '{}' BANNED for refusing to pay (bookingId={})",
                guestId, CamundaVars.getString(execution, "bookingId"));
    }
}