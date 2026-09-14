package main.delegate;

import main.entity.Listing;
import main.repository.ListingRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("loadListingForEditDelegate")
public class LoadListingForEditDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(LoadListingForEditDelegate.class);

    private final ListingRepository listingRepository;

    public LoadListingForEditDelegate(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long listingId = toLong(execution.getVariable("listingId"));
        if (listingId == null) {
            throw new IllegalStateException("listingId is null");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalStateException("Listing not found: " + listingId));

        execution.setVariable("title", listing.getTitle());
        execution.setVariable("address", listing.getAddress());
        execution.setVariable("description", listing.getDescription());
        execution.setVariable("price", listing.getPrice().doubleValue());

        log.info("Loaded listing {} for edit", listingId);
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