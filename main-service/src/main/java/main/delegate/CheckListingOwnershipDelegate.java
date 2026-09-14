package main.delegate;

import main.entity.Listing;
import main.entity.ListingStatus;
import main.repository.ListingRepository;
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
        Long listingId = toLong(execution.getVariable("listingId"));
        String userId = (String) execution.getVariable("initiatorUserId");

        execution.removeVariable("checkError");

        if (listingId == null) {
            log.warn("listingId is null in process {}", execution.getProcessInstanceId());
            execution.setVariable("checkError", ERROR_NOT_FOUND);
            return;
        }

        Listing listing = listingRepository.findById(listingId).orElse(null);

        if (listing == null || listing.getStatus() == ListingStatus.DELETED) {
            log.info("Listing {} not found", listingId);
            execution.setVariable("checkError", ERROR_NOT_FOUND);
            return;
        }

        if (!listing.getOwnerId().equals(userId)) {
            log.info("Listing {} does not belong to {}", listingId, userId);
            execution.setVariable("checkError", ERROR_NOT_OWNER);
            return;
        }

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            log.info("Listing {} is not AVAILABLE (status={})", listingId, listing.getStatus());
            execution.setVariable("checkError", ERROR_NOT_AVAILABLE);
            return;
        }

        execution.setVariable("listingTitle", listing.getTitle());
        execution.setVariable("listingAddress", listing.getAddress());

        log.info("Listing {} ownership check passed for user {}", listingId, userId);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}