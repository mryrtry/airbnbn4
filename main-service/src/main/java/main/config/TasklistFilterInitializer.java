package main.config;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.FilterService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.filter.Filter;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@Order(20)
public class TasklistFilterInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TasklistFilterInitializer.class);

    private static final String MY_TASKS_FILTER = "My Tasks";

    private final FilterService filterService;
    private final TaskService taskService;          // ← добавили
    private final IdentityService identityService;
    private final AuthorizationService authorizationService;

    public TasklistFilterInitializer(FilterService filterService,
                                     TaskService taskService,        // ← добавили
                                     IdentityService identityService,
                                     AuthorizationService authorizationService) {
        this.filterService = filterService;
        this.taskService = taskService;
        this.identityService = identityService;
        this.authorizationService = authorizationService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Tasklist filters...");

        createMyTasksFilter();
        grantFilterAccessToGroup("USER");
        grantFilterAccessToGroup("OWNER");
        grantFilterAccessToGroup("ADMIN");

        log.info("Tasklist filters initialized.");
    }

    private void createMyTasksFilter() {
        long existing = filterService.createFilterQuery()
                .filterName(MY_TASKS_FILTER)
                .count();
        if (existing > 0) {
            log.info("Filter '{}' already exists", MY_TASKS_FILTER);
            return;
        }

        TaskQuery query = taskService.createTaskQuery()
                .taskCandidateUserExpression("${currentUser()}")
                .includeAssignedTasks()
                .orderByTaskCreateTime()
                .desc();

        Filter filter = filterService.newTaskFilter(MY_TASKS_FILTER);
        filter.setQuery(query);
        filter.setOwner(null);
        filter.setProperties(new HashMap<>());
        filter.getProperties().put("description", "Tasks where I am candidate or assignee");
        filter.getProperties().put("color", "#4CAF50");
        filter.getProperties().put("priority", 10);

        filterService.saveFilter(filter);
        String filterId = filter.getId();

        log.info("Created global filter '{}' with id {}", MY_TASKS_FILTER, filterId);
    }

    private void grantFilterAccessToGroup(String groupId) {
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
        if (group == null) {
            log.warn("Group '{}' not found, skipping filter access", groupId);
            return;
        }

        boolean exists = authorizationService.createAuthorizationQuery()
                .groupIdIn(groupId)
                .resourceType(Resources.FILTER)
                .resourceId("*")
                .count() > 0;
        if (exists) {
            log.debug("FILTER access already granted for group {}", groupId);
            return;
        }

        Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        auth.setGroupId(groupId);
        auth.setResource(Resources.FILTER);
        auth.setResourceId("*");
        auth.setPermissions(new Permissions[]{Permissions.READ});
        authorizationService.saveAuthorization(auth);
        log.info("Granted FILTER READ to group {}", groupId);
    }
}