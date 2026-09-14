package main.delegate;

import main.entity.Listing;
import main.entity.ListingStatus;
import main.repository.ListingRepository;
import main.util.CamundaVars;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("checkListingOwnershipDelegate")
public class CheckListingOwnershipDelegate implements JavaDelegate {

    public static final String ERROR_NOT_FOUND = "NOT_FOUND";
    public static final String ERROR_NOT_OWNER = "NOT_OWNER";
    public static final String ERROR_NOT_AVAILABLE = "NOT_AVAILABLE";

    private static final Logger log = LoggerFactory.getLogger(CheckListingOwnershipDelegate.class);

    private final ListingRepository listingRepository;

    public CheckListingOwnershipDelegate(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.removeVariable("checkError");
        execution.removeVariable("processErrorCode");
        execution.removeVariable("processErrorMessage");

        Long listingId = CamundaVars.getLong(execution, "listingId");
        String userId = CamundaVars.getString(execution, "initiatorUserId");

        if (listingId == null) {
            setBusinessError(execution, ERROR_NOT_FOUND);
            return;
        }

        Listing listing = listingRepository.findById(listingId).orElse(null);
        String error = evaluate(listing, userId, listingId);
        if (error != null) {
            setBusinessError(execution, error);
            return;
        }

        execution.setVariable("listingTitle", listing.getTitle());
        execution.setVariable("listingAddress", listing.getAddress());
        log.info("Listing {} ownership check passed for user {}", listingId, userId);
    }

    private String evaluate(Listing listing, String userId, Long listingId) {
        if (listing == null || listing.getStatus() == ListingStatus.DELETED) {
            log.info("Listing {} not found", listingId);
            return ERROR_NOT_FOUND;
        }
        if (!listing.getOwnerId().equals(userId)) {
            log.info("Listing {} does not belong to {}", listingId, userId);
            return ERROR_NOT_OWNER;
        }
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            log.info("Listing {} is not AVAILABLE (status={})", listingId, listing.getStatus());
            return ERROR_NOT_AVAILABLE;
        }
        return null;
    }

    private void setBusinessError(DelegateExecution execution, String error) {
        execution.setVariable("checkError", error);
        execution.setVariable("processErrorCode", error);
        execution.setVariable("processErrorMessage", switch (error) {
            case ERROR_NOT_FOUND -> "Объявление с указанным ID не найдено.";
            case ERROR_NOT_OWNER -> "Объявление не принадлежит вам.";
            case ERROR_NOT_AVAILABLE -> "Объявление недоступно для изменения или удаления.";
            default -> "Операция с объявлением недоступна.";
        });
    }
}
