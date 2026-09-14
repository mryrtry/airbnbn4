package main.service;

import main.exception.ValidationException;
import org.camunda.bpm.engine.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBanService {

    private static final Logger log = LoggerFactory.getLogger(UserBanService.class);

    private static final String[] ROLES_TO_REMOVE = {"USER", "OWNER", "ADMIN"};
    private static final String ROLE_BANNED = "BANNED";

    private final IdentityService identityService;

    public UserBanService(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Transactional
    public void ban(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("guestId is null");
        }

        for (String role : ROLES_TO_REMOVE) {
            removeMembership(userId, role);
        }

        boolean alreadyBanned = identityService.createUserQuery()
                .userId(userId).memberOfGroup(ROLE_BANNED).count() > 0;
        if (!alreadyBanned) {
            identityService.createMembership(userId, ROLE_BANNED);
        }
    }

    private void removeMembership(String userId, String groupId) {
        try {
            long count = identityService.createUserQuery()
                    .userId(userId).memberOfGroup(groupId).count();
            if (count > 0) {
                identityService.deleteMembership(userId, groupId);
            }
        } catch (Exception ex) {
            log.debug("Failed to remove membership {} from {}", groupId, userId, ex);
        }
    }
}