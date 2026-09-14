package main.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("createResolutionWindowDelegate")
public class CreateResolutionWindowDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreateResolutionWindowDelegate.class);

    /** Окно живёт столько же, сколько timer в BPMN (PT1M). */
    public static final long WINDOW_DURATION_SECONDS = 60L;

    @Override
    public void execute(DelegateExecution execution) {
        Instant openedAt = Instant.now();
        Instant deadline = openedAt.plusSeconds(WINDOW_DURATION_SECONDS);

        execution.setVariable("windowOpenedAt", openedAt.toString());
        execution.setVariable("windowDeadline", deadline.toString());

        log.info("[RESOLUTION] Window opened for bookingId={}, deadline={}",
                execution.getVariable("bookingId"), deadline);
    }
}