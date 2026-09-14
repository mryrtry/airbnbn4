package main.delegate;

import main.entity.Listing;
import main.entity.ListingStatus;
import main.repository.ListingRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component("deleteListingDelegate")
public class DeleteListingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(DeleteListingDelegate.class);

    private final ListingRepository listingRepository;

    public DeleteListingDelegate(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        execution.removeVariable("deleteError");

        Long listingId = toLong(execution.getVariable("listingId"));
        if (listingId == null) {
            execution.setVariable("deleteError", "listingId is null");
            return;
        }

        try {
            Listing listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new IllegalStateException("Listing not found: " + listingId));

            listing.setStatus(ListingStatus.DELETED);
            listing.setDeletedAt(Instant.now());
            listing.setUpdatedAt(Instant.now());
            listingRepository.save(listing);

            log.info("Listing {} soft-deleted", listingId);

        } catch (Exception e) {
            log.error("Failed to delete listing {}", listingId, e);
            execution.setVariable("deleteError",
                    e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
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