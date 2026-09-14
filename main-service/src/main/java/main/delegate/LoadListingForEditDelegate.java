package main.delegate;

import main.entity.Listing;
import main.exception.EntityNotFoundException;
import main.repository.ListingRepository;
import main.util.CamundaVars;
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
        Long listingId = CamundaVars.getLong(execution, "listingId");
        if (listingId == null) {
            throw new EntityNotFoundException("listingId is null");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + listingId));

        execution.setVariable("title", listing.getTitle());
        execution.setVariable("address", listing.getAddress());
        execution.setVariable("description", listing.getDescription());
        execution.setVariable("price", listing.getPrice().doubleValue());

        log.info("Loaded listing {} for edit", listingId);
    }
}