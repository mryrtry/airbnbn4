package main.scheduler;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ResolutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ResolutionScheduler.class);

    private final RuntimeService runtimeService;

    public ResolutionScheduler(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Scheduled(fixedRate = 30_000)
    public void logOpenWindows() {
        List<ProcessInstance> active = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("resolution-lifecycle")
                .list();

        if (active.isEmpty()) {
            log.info("[RESOLUTION-SCHEDULER] no open resolution windows");
            return;
        }

        log.info("[RESOLUTION-SCHEDULER] open resolution windows: {}", active.size());
        for (ProcessInstance pi : active) {
            Object deadlineRaw = runtimeService.getVariable(pi.getId(), "windowDeadline");
            long minutesLeft = -1;
            if (deadlineRaw != null) {
                try {
                    Instant deadline = Instant.parse(deadlineRaw.toString());
                    minutesLeft = Duration.between(Instant.now(), deadline).toMinutes();
                } catch (Exception ignored) { }
            }
            log.info("[RESOLUTION-SCHEDULER] pid={}, bookingId={}, businessKey={}, minutesLeft={}",
                    pi.getId(),
                    runtimeService.getVariable(pi.getId(), "bookingId"),
                    pi.getBusinessKey(),
                    minutesLeft);
        }
    }
}