package main.service;

import main.util.UserEmails;
import org.camunda.bpm.engine.IdentityService;
import org.springframework.stereotype.Service;

@Service
public class NotificationContextService {

    private static final String RECIPIENT_GUEST = "GUEST";

    private final IdentityService identityService;

    public NotificationContextService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public String resolveRecipient(String recipient, String guestId, String ownerId) {
        return RECIPIENT_GUEST.equals(recipient) ? guestId : ownerId;
    }

    public String resolveEmail(String username) {
        return UserEmails.resolve(identityService, username);
    }
}