package main.config;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(10)
public class CamundaAuthorizationInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CamundaAuthorizationInitializer.class);

    /**
     * Какие процессы видны и стартуемы для каждой группы.
     * Camunda объединяет права по всем группам пользователя (union).
     */
    private static final Map<String, List<String>> PROCESSES_BY_GROUP = Map.of(
            "USER", List.of("booking-lifecycle"),
            "OWNER", List.of("listing-create", "listing-update", "listing-delete"),
            "ADMIN", List.of("listing-create", "listing-update", "listing-delete", "booking-lifecycle")
    );

    private final AuthorizationService authorizationService;

    public CamundaAuthorizationInitializer(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Camunda authorizations...");

        removeLegacyWildcardAuthorizations();

        for (String group : new String[]{"USER", "OWNER", "ADMIN"}) {
            grantTaskPermissions(group);
            grantProcessDefinitionPermissions(group);
            grantProcessInstancePermissions(group);
            grantApplicationAccess(group);
            grantGroupMembershipPermission(group);
        }

        // BANNED — всем всё запрещаем через REVOKE
        revokeAllForBanned();

        log.info("Camunda authorizations initialized.");
    }

    private void removeLegacyWildcardAuthorizations() {
        authorizationService.createAuthorizationQuery()
                .resourceType(Resources.APPLICATION)
                .list()
                .stream()
                .filter(a -> "*".equals(a.getResourceId()))
                .forEach(a -> {
                    authorizationService.deleteAuthorization(a.getId());
                    log.info("Removed legacy APPLICATION wildcard: group={}", a.getGroupId());
                });

        authorizationService.createAuthorizationQuery()
                .resourceType(Resources.PROCESS_DEFINITION)
                .list()
                .stream()
                .filter(a -> "*".equals(a.getResourceId()))
                .forEach(a -> {
                    authorizationService.deleteAuthorization(a.getId());
                    log.info("Removed legacy PROCESS_DEFINITION wildcard: group={}", a.getGroupId());
                });
    }

    // ──────────────────────── BANNED ────────────────────────

    /**
     * REVOKE для группы BANNED на все ключевые ресурсы.
     * REVOKE в Camunda перебивает любой GRANT, даже если юзер в других группах.
     */
    private void revokeAllForBanned() {
        String group = "BANNED";

        // 1. Process Definition — запретить видеть и стартовать ЛЮБЫЕ процессы
        revoke(group, Resources.PROCESS_DEFINITION, "*", new Permissions[]{
                Permissions.READ,
                Permissions.CREATE_INSTANCE,
                Permissions.UPDATE,
                Permissions.DELETE
        });

        // 2. Process Instance — запретить работать с инстансами
        revoke(group, Resources.PROCESS_INSTANCE, "*", new Permissions[]{
                Permissions.CREATE,
                Permissions.READ,
                Permissions.UPDATE,
                Permissions.DELETE
        });

        // 3. Task — запретить видеть и брать задачи
        revoke(group, Resources.TASK, "*", new Permissions[]{
                Permissions.READ,
                Permissions.UPDATE,
                Permissions.TASK_ASSIGN,
                Permissions.TASK_WORK
        });

        // 4. Application — запретить вход в Cockpit/Tasklist/Admin
        for (String app : new String[]{"cockpit", "tasklist", "admin", "welcome"}) {
            revoke(group, Resources.APPLICATION, app, new Permissions[]{Permissions.ACCESS});
        }

        log.info("[BANNED] Revoked all permissions for group {}", group);
    }

    private void revoke(String group, Resources resource, String resourceId, Permissions[] permissions) {
        // удаляем прошлые REVOKE для этой группы+ресурса+id, чтобы не плодить дубли
        authorizationService.createAuthorizationQuery()
                .groupIdIn(group)
                .resourceType(resource)
                .resourceId(resourceId)
                .list()
                .stream()
                .filter(a -> a.getAuthorizationType() == Authorization.AUTH_TYPE_REVOKE)
                .forEach(a -> authorizationService.deleteAuthorization(a.getId()));

        Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_REVOKE);
        auth.setGroupId(group);
        auth.setResource(resource);
        auth.setResourceId(resourceId);
        auth.setPermissions(permissions);
        authorizationService.saveAuthorization(auth);

        log.info("[BANNED] Revoked {} on {}:{}", permissions.length, resource.resourceName(), resourceId);
    }

    // ──────────────────────── GRANT (USER/OWNER/ADMIN) ────────────────────────

    private void grantTaskPermissions(String group) {
        authorizationService.createAuthorizationQuery()
                .groupIdIn(group)
                .resourceType(Resources.TASK)
                .list()
                .forEach(a -> {
                    authorizationService.deleteAuthorization(a.getId());
                    log.info("[TASK] Removed old auth id={} for group {}", a.getId(), group);
                });

        Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        auth.setGroupId(group);
        auth.setResource(Resources.TASK);
        auth.setResourceId("*");
        auth.setPermissions(new Permissions[]{
                Permissions.TASK_ASSIGN
        });
        authorizationService.saveAuthorization(auth);
        log.info("[TASK] Granted TASK_ASSIGN to group {}", group);
    }

    private void grantProcessDefinitionPermissions(String group) {
        List<String> processes = PROCESSES_BY_GROUP.get(group);
        if (processes == null) return;

        Permissions[] perms = new Permissions[]{
                Permissions.READ,
                Permissions.CREATE_INSTANCE
        };

        for (String processKey : processes) {
            boolean exists = authorizationService.createAuthorizationQuery()
                    .groupIdIn(group)
                    .resourceType(Resources.PROCESS_DEFINITION)
                    .resourceId(processKey)
                    .count() > 0;
            if (exists) {
                log.debug("[PROCESS_DEFINITION] {} already authorized for group {}", processKey, group);
                continue;
            }

            Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            auth.setGroupId(group);
            auth.setResource(Resources.PROCESS_DEFINITION);
            auth.setResourceId(processKey);
            auth.setPermissions(perms);
            authorizationService.saveAuthorization(auth);
            log.info("[PROCESS_DEFINITION] Granted {} [READ, CREATE_INSTANCE] to group {}", processKey, group);
        }
    }

    private void grantProcessInstancePermissions(String group) {
        boolean exists = authorizationService.createAuthorizationQuery()
                .groupIdIn(group)
                .resourceType(Resources.PROCESS_INSTANCE)
                .count() > 0;
        if (exists) {
            log.debug("[PROCESS_INSTANCE] already granted for group {}", group);
            return;
        }

        Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        auth.setGroupId(group);
        auth.setResource(Resources.PROCESS_INSTANCE);
        auth.setResourceId("*");
        auth.setPermissions(new Permissions[]{
                Permissions.CREATE,
                Permissions.READ,
                Permissions.UPDATE
        });
        authorizationService.saveAuthorization(auth);
        log.info("[PROCESS_INSTANCE] Granted permissions to group {}", group);
    }

    private void grantApplicationAccess(String group) {
        String[] applications = {"cockpit", "tasklist", "admin", "welcome"};

        for (String app : applications) {
            boolean exists = authorizationService.createAuthorizationQuery()
                    .groupIdIn(group)
                    .resourceType(Resources.APPLICATION)
                    .resourceId(app)
                    .count() > 0;
            if (exists) continue;

            Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            auth.setGroupId(group);
            auth.setResource(Resources.APPLICATION);
            auth.setResourceId(app);
            auth.setPermissions(new Permissions[]{Permissions.ACCESS});
            authorizationService.saveAuthorization(auth);
            log.info("[APPLICATION] Granted {} ACCESS to group {}", app, group);
        }
    }

    private void grantGroupMembershipPermission(String group) {
        grantGroupRead(group);
        grantGroupMembershipCreateDelete(group);
    }

    private void grantGroupRead(String group) {
        boolean exists = authorizationService.createAuthorizationQuery()
                .groupIdIn(group)
                .resourceType(Resources.GROUP)
                .resourceId("*")
                .count() > 0;
        if (exists) return;

        Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        auth.setGroupId(group);
        auth.setResource(Resources.GROUP);
        auth.setResourceId("*");
        auth.setPermissions(new Permissions[]{
                Permissions.READ
        });
        authorizationService.saveAuthorization(auth);
        log.info("[GROUP] Granted READ to group {}", group);
    }

    private void grantGroupMembershipCreateDelete(String group) {
        boolean exists = authorizationService.createAuthorizationQuery()
                .groupIdIn(group)
                .resourceType(Resources.GROUP_MEMBERSHIP)
                .resourceId("*")
                .count() > 0;
        if (exists) return;

        Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        auth.setGroupId(group);
        auth.setResource(Resources.GROUP_MEMBERSHIP);
        auth.setResourceId("*");
        auth.setPermissions(new Permissions[]{
                Permissions.CREATE,
                Permissions.DELETE
        });
        authorizationService.saveAuthorization(auth);
        log.info("[GROUP_MEMBERSHIP] Granted CREATE+DELETE to group {}", group);
    }
}