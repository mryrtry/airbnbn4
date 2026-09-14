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

@Component("updateListingDelegate")
public class UpdateListingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(UpdateListingDelegate.class);

    private final ListingLifecycleService listingLifecycleService;

    public UpdateListingDelegate(ListingLifecycleService listingLifecycleService) {
        this.listingLifecycleService = listingLifecycleService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.removeVariable("updateError");

        Long listingId = CamundaVars.getLong(execution, "listingId");

        try {
            listingLifecycleService.update(
                    listingId,
                    CamundaVars.getString(execution, "title"),
                    CamundaVars.getString(execution, "address"),
                    CamundaVars.getString(execution, "description"),
                    toBigDecimal(execution.getVariable("price"))
            );
        } catch (ValidationException ex) {
            log.warn("Listing update validation failed: {}", ex.getMessage());
            execution.setVariable("updateError", ex.getMessage());
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