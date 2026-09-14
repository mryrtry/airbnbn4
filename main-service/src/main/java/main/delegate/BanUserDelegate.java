package main.delegate;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("banUserDelegate")
public class BanUserDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(BanUserDelegate.class);

    private final IdentityService identityService;

    public BanUserDelegate(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String guestId = (String) execution.getVariable("guestId");
        if (guestId == null) {
            throw new IllegalStateException("guestId is null");
        }

        // убираем из USER и OWNER, добавляем в BANNED
        removeMembership(guestId, "USER");
        removeMembership(guestId, "OWNER");

        boolean alreadyBanned = identityService.createUserQuery()
                .userId(guestId).memberOfGroup("BANNED").count() > 0;
        if (!alreadyBanned) {
            identityService.createMembership(guestId, "BANNED");
        }

        execution.setVariable("complaintStatus", "DECLINED");
        execution.setVariable("windowStatus", "CLOSED");
        execution.setVariable("windowClosedAt", Instant.now().toString());
        execution.setVariable("userBanned", true);

        log.warn("[RESOLUTION] User '{}' BANNED for refusing to pay (bookingId={})",
                guestId, execution.getVariable("bookingId"));
    }

    private void removeMembership(String userId, String groupId) {
        try {
            long count = identityService.createUserQuery()
                    .userId(userId).memberOfGroup(groupId).count();
            if (count > 0) {
                identityService.deleteMembership(userId, groupId);
            }
        } catch (Exception e) {
            log.debug("Failed to remove membership {} from {}", groupId, userId, e);
        }
    }
}