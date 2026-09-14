package main.config;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class GroupInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GroupInitializer.class);

    private static final String[][] GROUPS = {
            {"USER", "User"},
            {"OWNER", "Owner"},
            {"ADMIN", "Admin"},
            {"BANNED", "Banned"}
    };

    private final IdentityService identityService;

    public GroupInitializer(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Camunda groups...");
        for (String[] g : GROUPS) {
            String id = g[0];
            String name = g[1];
            if (identityService.createGroupQuery().groupId(id).count() > 0) continue;
            Group group = identityService.newGroup(id);
            group.setName(name);
            identityService.saveGroup(group);
            log.info("Created group '{}' ({})", id, name);
        }
        log.info("Camunda groups initialized.");
    }
}