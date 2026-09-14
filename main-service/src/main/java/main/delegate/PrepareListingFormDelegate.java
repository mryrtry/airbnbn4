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

import java.util.List;
import java.util.stream.Collectors;

@Component("prepareListingFormDelegate")
public class PrepareListingFormDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PrepareListingFormDelegate.class);

    private final ListingRepository listingRepository;

    public PrepareListingFormDelegate(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long listingId = CamundaVars.getLong(execution, "listingId");

        if (listingId != null) {
            Listing listing = listingRepository.findById(listingId).orElse(null);
            if (listing != null) {
                execution.setVariable("formListingId", listing.getId());
                execution.setVariable("formListingTitle", listing.getTitle());
                execution.setVariable("formListingAddress", listing.getAddress());
                execution.setVariable("formListingDescription",
                        listing.getDescription() != null ? listing.getDescription() : "");
                execution.setVariable("formListingPrice", listing.getPrice().toString());
            }
        }

        List<Listing> available = listingRepository.findByStatus(ListingStatus.AVAILABLE);
        String options = available.stream()
                .map(l -> l.getId() + ": " + l.getTitle() + " (" + l.getAddress() + ")")
                .collect(Collectors.joining("; "));

        execution.setVariable("formAvailableListings", options.isEmpty() ? "—" : options);
        log.info("PrepareListingForm: {} available listings", available.size());
    }
}