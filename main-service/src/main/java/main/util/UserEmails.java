package main.util;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.User;

public final class UserEmails {

    private UserEmails() {
    }

    public static String resolve(IdentityService identityService, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        User user = identityService.createUserQuery().userId(username).singleResult();
        return user != null ? user.getEmail() : null;
    }
}