package main.dto;

import java.util.List;
import java.util.Map;

public record ProcessStatusDto(
        String processInstanceId,
        String processDefinitionKey,
        String businessKey,
        boolean active,
        String endEvent,
        String processResult,
        String startUserId,
        String startTime,
        String endTime,
        Map<String, Object> variables,
        List<ActivityDto> activities
) {
    public record ActivityDto(
            String activityId,
            String activityName,
            String activityType,
            String startTime,
            String endTime
    ) {
    }
}