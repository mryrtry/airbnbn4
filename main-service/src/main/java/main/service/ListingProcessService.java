package main.service;

import main.dto.ProcessStartResult;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ListingProcessService {

    private static final String PROCESS_CREATE = "listing-create";
    private static final String PROCESS_UPDATE = "listing-update";
    private static final String PROCESS_DELETE = "listing-delete";

    private final RuntimeService runtimeService;

    public ListingProcessService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public ProcessStartResult startCreate(String userId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("initiatorUserId", userId);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_CREATE, vars);
        return new ProcessStartResult(instance.getId(), "CREATE", null, "started");
    }

    public ProcessStartResult startUpdate(String userId, Long listingId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("initiatorUserId", userId);
        vars.put("listingId", listingId);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_UPDATE, "listing-" + listingId, vars);
        return new ProcessStartResult(instance.getId(), "UPDATE", listingId, "started");
    }

    public ProcessStartResult startDelete(String userId, Long listingId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("initiatorUserId", userId);
        vars.put("listingId", listingId);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_DELETE, "listing-" + listingId, vars);
        return new ProcessStartResult(instance.getId(), "DELETE", listingId, "started");
    }
}