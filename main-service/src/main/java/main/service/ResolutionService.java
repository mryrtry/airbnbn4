package main.service;

import main.exception.ConflictException;
import main.exception.ForbiddenException;
import main.exception.ProcessException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionService.class);

    private static final String PROCESS_KEY = "resolution-lifecycle";
    private static final String BUSINESS_KEY_PREFIX = "resolution-booking-";
    private static final String TASK_FILE_COMPLAINT = "Task_OwnerFileComplaint";
    private static final String VAR_OWNER_ID = "ownerId";
    private static final String VAR_REASON = "reason";
    private static final String VAR_OWNER_COMMENT = "ownerComment";

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public ResolutionService(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Transactional
    public void fileComplaint(Long bookingId, String username, String reason, String ownerComment) {
        ProcessInstance pi = findOpenWindow(bookingId);
        verifyOwner(pi, username);
        Task task = findActiveComplaintTask(pi.getId());

        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_REASON, reason);
        if (ownerComment != null && !ownerComment.isBlank()) {
            vars.put(VAR_OWNER_COMMENT, ownerComment);
        }

        try {
            taskService.complete(task.getId(), vars);
        } catch (Exception ex) {
            throw new ProcessException("Failed to file complaint for booking " + bookingId, ex);
        }

        log.info("[RESOLUTION] Complaint filed: bookingId={}, owner={}", bookingId, username);
    }

    private ProcessInstance findOpenWindow(Long bookingId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceBusinessKey(BUSINESS_KEY_PREFIX + bookingId)
                .singleResult();

        if (pi == null) {
            throw new ConflictException(
                    "Resolution window is not open for booking " + bookingId);
        }
        return pi;
    }

    private void verifyOwner(ProcessInstance pi, String username) {
        Object ownerId = runtimeService.getVariable(pi.getId(), VAR_OWNER_ID);
        if (ownerId == null || !ownerId.equals(username)) {
            throw new ForbiddenException("Not the owner of this booking");
        }
    }

    private Task findActiveComplaintTask(String processInstanceId) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(TASK_FILE_COMPLAINT)
                .singleResult();

        if (task == null) {
            throw new ConflictException(
                    "Complaint window is no longer accepting complaints");
        }
        return task;
    }
}