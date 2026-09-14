package main.delegate;

import main.service.ListingLifecycleService;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("deleteListingDelegate")
public class DeleteListingDelegate implements JavaDelegate {

    private final ListingLifecycleService listingLifecycleService;

    public DeleteListingDelegate(ListingLifecycleService listingLifecycleService) {
        this.listingLifecycleService = listingLifecycleService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.removeVariable("deleteError");
        execution.removeVariable("processErrorCode");
        execution.removeVariable("processErrorMessage");

        Long listingId = CamundaVars.getLong(execution, "listingId");
        if (listingId == null) {
            execution.setVariable("deleteError", "listingId is null");
            execution.setVariable("processErrorCode", "LISTING_ID_REQUIRED");
            execution.setVariable("processErrorMessage", "Необходимо выбрать объявление для удаления.");
            return;
        }

        listingLifecycleService.softDelete(listingId);
    }
}
