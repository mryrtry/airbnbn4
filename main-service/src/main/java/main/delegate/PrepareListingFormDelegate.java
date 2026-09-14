package main.delegate;

import main.entity.Listing;
import main.entity.ListingStatus;
import main.repository.ListingRepository;
import main.util.CamundaVars;
import main.util.UserNames;
import org.camunda.bpm.engine.IdentityService;
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
    private final IdentityService identityService;

    public PrepareListingFormDelegate(ListingRepository listingRepository,
                                      IdentityService identityService) {
        this.listingRepository = listingRepository;
        this.identityService = identityService;
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

        String ownerId = CamundaVars.getString(execution, "initiatorUserId");
        List<Listing> available = ownerId == null
                ? List.of()
                : listingRepository.findByOwnerIdAndStatus(ownerId, ListingStatus.AVAILABLE);
        String options = available.stream()
                .map(this::formatListing)
                .collect(Collectors.joining("\n\n"));

        execution.setVariable("formAvailableListings", options.isEmpty() ? "—" : options);
        log.info("PrepareListingForm: {} available listings", available.size());
    }

    private String formatListing(Listing listing) {
        return "ID: " + listing.getId()
                + "\nНазвание: " + listing.getTitle()
                + "\nАдрес: " + listing.getAddress()
                + "\nОписание: " + valueOrDash(listing.getDescription())
                + "\nВладелец: " + UserNames.resolve(identityService, listing.getOwnerId())
                + "\nЦена за ночь: " + listing.getPrice().toPlainString();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
