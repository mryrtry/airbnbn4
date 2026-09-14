package main.controller;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resolutions")
public class ResolutionController {

    private static final Logger log = LoggerFactory.getLogger(ResolutionController.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public ResolutionController(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('RESOLUTION_OPEN')")
    public ResponseEntity<Map<String, Object>> fileComplaint(
            @RequestBody FileComplaintRequest req,
            Authentication auth) {

        if (req.bookingId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "bookingId is required"));
        }
        if (req.reason() == null || req.reason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason is required"));
        }

        String businessKey = "resolution-booking-" + req.bookingId();
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("resolution-lifecycle")
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (pi == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "resolution window is not open for this booking"));
        }

        // проверяем, что это владелец
        Object ownerId = runtimeService.getVariable(pi.getId(), "ownerId");
        if (ownerId == null || !ownerId.equals(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "not the owner of this booking"));
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .taskDefinitionKey("Task_OwnerFileComplaint")
                .singleResult();

        if (task == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "complaint window is no longer accepting complaints"));
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("reason", req.reason());
        if (req.ownerComment() != null) vars.put("ownerComment", req.ownerComment());

        taskService.complete(task.getId(), vars);
        log.info("[RESOLUTION] Complaint filed via REST: bookingId={}, owner={}",
                req.bookingId(), auth.getName());

        return ResponseEntity.ok(Map.of(
                "status", "filed",
                "processInstanceId", pi.getId(),
                "bookingId", req.bookingId()));
    }

    public record FileComplaintRequest(Long bookingId, String reason, String ownerComment) {
    }
}