package main.util;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.User;

import java.util.stream.Stream;

public final class UserNames {

    private UserNames() {
    }

    public static String resolve(IdentityService identityService, String username) {
        if (username == null || username.isBlank()) {
            return "—";
        }

        User user = identityService.createUserQuery().userId(username).singleResult();
        if (user == null) {
            return username;
        }

        String fullName = Stream.of(user.getFirstName(), user.getLastName())
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        return fullName.isBlank() ? username : fullName + " (" + username + ")";
    }
}
