package main.delegate;

import main.security.PermissionService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.identity.User;
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
        String username = resolveUsername(execution);

        if (username == null) {
            log.warn("[INIT] Cannot resolve initiatorUserId for process {}",
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

        User user = identityService.createUserQuery().userId(username).singleResult();
        if (user != null && user.getEmail() != null) {
            execution.setVariable("initiatorEmail", user.getEmail());
        }

        log.info("[INIT] Success: initiatorUserId='{}', roles={}, permissions={}",
                username, roles, permissions.size());
    }

    private String resolveUsername(DelegateExecution execution) {
        String fromVar = CamundaVars.getString(execution, "initiatorUserId");
        if (fromVar != null && !fromVar.isBlank()) {
            return fromVar;
        }

        String fromAuth = currentAuthenticationUser();
        if (fromAuth != null) {
            return fromAuth;
        }

        return historicStartUser(execution.getProcessInstanceId());
    }

    private String currentAuthenticationUser() {
        try {
            var auth = identityService.getCurrentAuthentication();
            if (auth != null && auth.getUserId() != null && !auth.getUserId().isBlank()) {
                return auth.getUserId();
            }
        } catch (Exception ex) {
            log.debug("[INIT] getCurrentAuthentication failed: {}", ex.getMessage());
        }
        return null;
    }

    private String historicStartUser(String processInstanceId) {
        try {
            HistoricProcessInstance historic = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (historic != null && historic.getStartUserId() != null
                    && !historic.getStartUserId().isBlank()) {
                return historic.getStartUserId();
            }
        } catch (Exception ex) {
            log.debug("[INIT] historyService query failed: {}", ex.getMessage());
        }
        return null;
    }
}