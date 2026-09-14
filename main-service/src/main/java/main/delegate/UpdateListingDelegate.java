package main.delegate;

import main.entity.Listing;
import main.repository.ListingRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Component("updateListingDelegate")
public class UpdateListingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(UpdateListingDelegate.class);

    private final ListingRepository listingRepository;

    public UpdateListingDelegate(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        execution.removeVariable("updateError");

        Long listingId = toLong(execution.getVariable("listingId"));
        if (listingId == null) {
            execution.setVariable("updateError", "listingId is null");
            return;
        }

        try {
            Listing listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new IllegalStateException("Listing not found: " + listingId));

            String title = (String) execution.getVariable("title");
            String address = (String) execution.getVariable("address");
            String description = (String) execution.getVariable("description");
            Object priceObj = execution.getVariable("price");

            if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
            if (address == null || address.isBlank()) throw new IllegalArgumentException("address is required");
            if (priceObj == null) throw new IllegalArgumentException("price is required");

            BigDecimal price = priceObj instanceof BigDecimal bd
                    ? bd
                    : new BigDecimal(priceObj.toString());
            if (price.signum() < 0) throw new IllegalArgumentException("price must be non-negative");

            listing.setTitle(title);
            listing.setAddress(address);
            listing.setDescription(description);
            listing.setPrice(price);
            listing.setUpdatedAt(Instant.now());
            listingRepository.save(listing);

            log.info("Listing {} updated", listingId);

        } catch (Exception e) {
            log.error("Failed to update listing {}", listingId, e);
            execution.setVariable("updateError",
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