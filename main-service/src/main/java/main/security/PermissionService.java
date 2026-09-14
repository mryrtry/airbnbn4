package main.security;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service("permissionService")
public class PermissionService {

	private final IdentityService identityService;

	private final DmnEngine dmnEngine;

	private final DmnDecision permissionsDecision;

	public PermissionService(IdentityService identityService, DmnEngine dmnEngine) {
		this.identityService = identityService;
		this.dmnEngine = dmnEngine;

		try (InputStream stream = getClass().getClassLoader().getResourceAsStream("permissions.dmn")) {
			if (stream == null) {
				throw new IllegalStateException("permissions.dmn not found in classpath");
			}
			this.permissionsDecision = dmnEngine.parseDecision("permissions", stream);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load permissions.dmn", ex);
		}
	}

	public boolean hasPermission(String permission) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return false;
		}
		return hasPermission(auth.getName(), permission);
	}

	public boolean hasPermission(String username, String permission) {
		return getPermissions(username).contains(permission);
	}

	public List<String> getPermissions(String username) {
		List<String> roles = identityService.createGroupQuery().groupMember(username).list().stream().map(Group::getId).filter(g -> !"camunda-admin".equals(g)).toList();

		Set<String> permissions = new HashSet<>();
		for (String role : roles) {
			VariableMap vars = Variables.createVariables().putValue("role", role);
			DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(permissionsDecision, vars);
			permissions.addAll(result.collectEntries("permission"));
		}

		return new ArrayList<>(permissions);
	}

	public List<String> getRoles(String username) {
		return identityService.createGroupQuery().groupMember(username).list().stream().map(Group::getId).filter(g -> !"camunda-admin".equals(g)).toList();
	}

}