package main.delegate;

import main.security.PermissionService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("initUserContextDelegate")
public class InitUserContextDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(InitUserContextDelegate.class);

    private final IdentityService identityService;
    private final PermissionService permissionService;
    private final HistoryService historyService;

    public InitUserContextDelegate(IdentityService identityService,
                                   PermissionService permissionService,
                                   HistoryService historyService) {
        this.identityService = identityService;
        this.permissionService = permissionService;
        this.historyService = historyService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String username = null;

        // 1. Переменная процесса (REST, форма)
        Object var = execution.getVariable("initiatorUserId");
        if (var instanceof String s && !s.isBlank()) {
            username = s;
            log.info("[INIT] initiatorUserId from variable: {}", username);
        }

        // 2. Camunda Authentication (Cockpit, Tasklist, REST с setAuthentication)
        if (username == null) {
            try {
                var auth = identityService.getCurrentAuthentication();
                if (auth != null && auth.getUserId() != null && !auth.getUserId().isBlank()) {
                    username = auth.getUserId();
                    log.info("[INIT] initiatorUserId from current authentication: {}", username);
                }
            } catch (Exception e) {
                log.debug("[INIT] getCurrentAuthentication failed: {}", e.getMessage());
            }
        }

        // 3. Historic process instance startUserId
        if (username == null) {
            try {
                HistoricProcessInstance historic = historyService
                        .createHistoricProcessInstanceQuery()
                        .processInstanceId(execution.getProcessInstanceId())
                        .singleResult();
                if (historic != null && historic.getStartUserId() != null
                        && !historic.getStartUserId().isBlank()) {
                    username = historic.getStartUserId();
                    log.info("[INIT] initiatorUserId from historic startUserId: {}", username);
                }
            } catch (Exception e) {
                log.debug("[INIT] historyService query failed: {}", e.getMessage());
            }
        }

        // Не нашли — логируем и выходим
        if (username == null || username.isBlank()) {
            log.warn("[INIT] Cannot resolve initiatorUserId for process {}. " +
                            "Variables, current auth, historic startUserId — all empty.",
                    execution.getProcessInstanceId());
            execution.setVariable("userRoles", new ArrayList<String>());
            execution.setVariable("userPermissions", new ArrayList<String>());
            return;
        }

        execution.setVariable("initiatorUserId", username);

        List<String> roles = new ArrayList<>(permissionService.getRoles(username));
        List<String> permissions = new ArrayList<>(permissionService.getPermissions(username));

        execution.setVariable("userRoles", roles);
        execution.setVariable("userPermissions", permissions);

        var user = identityService.createUserQuery().userId(username).singleResult();
        if (user != null && user.getEmail() != null) {
            execution.setVariable("initiatorEmail", user.getEmail());
        }

        log.info("[INIT] Success: initiatorUserId='{}', roles={}, permissions={}",
                username, roles, permissions.size());
    }
}