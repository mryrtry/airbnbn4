package main.delegate;

import main.entity.ListingStatus;
import main.repository.ListingRepository;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("checkOwnerAvailableListingsDelegate")
public class CheckOwnerAvailableListingsDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CheckOwnerAvailableListingsDelegate.class);
    private final ListingRepository listingRepository;

    public CheckOwnerAvailableListingsDelegate(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String ownerId = CamundaVars.getString(execution, "initiatorUserId");
        boolean hasAvailableListings = ownerId != null
                && listingRepository.existsByOwnerIdAndStatus(ownerId, ListingStatus.AVAILABLE);

        execution.setVariable("hasAvailableListings", hasAvailableListings);
        if (!hasAvailableListings) {
            execution.setVariable("processErrorCode", "NO_AVAILABLE_LISTINGS");
            execution.setVariable("processErrorMessage", unavailableMessage(execution.getProcessDefinitionId()));
        }
        log.info("Owner {} has AVAILABLE listings: {}", ownerId, hasAvailableListings);
    }

    private String unavailableMessage(String processDefinitionId) {
        if (processDefinitionId != null && processDefinitionId.startsWith("listing-delete:")) {
            return "У вас нет доступных объявлений для удаления.";
        }
        return "У вас нет доступных объявлений для изменения.";
    }
}
