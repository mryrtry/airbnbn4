package main.service;

import main.dto.UserInfo;
import main.exception.EntityNotFoundException;
import main.exception.UserAlreadyExistsException;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CamundaUserService {

    private final IdentityService identityService;

    public CamundaUserService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public UserInfo register(String username, String password, String email, String firstName, String lastName) {

        if (exists(username)) {
            throw new UserAlreadyExistsException("User already exists: " + username);
        }

        User user = identityService.newUser(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        identityService.saveUser(user);

        identityService.createMembership(username, "USER");

        return getInfo(username);
    }

    public boolean exists(String username) {
        return identityService.createUserQuery().userId(username).count() > 0;
    }

    public boolean checkPassword(String username, String password) {
        return identityService.checkPassword(username, password);
    }

    public List<String> getGroups(String username) {
        return identityService.createGroupQuery().groupMember(username).list().stream().map(Group::getId).toList();
    }

    public UserInfo getInfo(String username) {
        User user = identityService.createUserQuery().userId(username).singleResult();
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + username);
        }
        return new UserInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), getGroups(username));
    }

    public UserInfo becomeOwner(String username) {
        if (!exists(username)) {
            throw new EntityNotFoundException("User not found: " + username);
        }

        long count = identityService.createUserQuery()
                .userId(username)
                .memberOfGroup("OWNER")
                .count();

        if (count == 0) {
            identityService.createMembership(username, "OWNER");
        }

        return getInfo(username);
    }

}