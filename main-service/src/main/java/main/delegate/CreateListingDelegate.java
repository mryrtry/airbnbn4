package main.delegate;

import main.exception.ValidationException;
import main.service.ListingLifecycleService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("createListingDelegate")
public class CreateListingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreateListingDelegate.class);

    private final ListingLifecycleService listingLifecycleService;

    public CreateListingDelegate(ListingLifecycleService listingLifecycleService) {
        this.listingLifecycleService = listingLifecycleService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.removeVariable("createError");

        try {
            Long listingId = listingLifecycleService.create(
                    execution.getProcessInstanceId(),
                    CamundaVars.getString(execution, "initiatorUserId"),
                    CamundaVars.getString(execution, "title"),
                    CamundaVars.getString(execution, "address"),
                    CamundaVars.getString(execution, "description"),
                    toBigDecimal(execution.getVariable("price"))
            );
            execution.setVariable("listingId", listingId);

        } catch (ValidationException ex) {
            log.warn("Listing creation validation failed: {}", ex.getMessage());
            execution.setVariable("createError", ex.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}