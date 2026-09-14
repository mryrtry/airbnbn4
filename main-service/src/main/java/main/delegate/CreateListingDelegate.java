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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Component("createListingDelegate")
public class CreateListingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreateListingDelegate.class);

    private final ListingRepository listingRepository;

    public CreateListingDelegate(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        // Идемпотентность
        Optional<Listing> existing = listingRepository.findByProcessInstanceId(processInstanceId);
        if (existing.isPresent()) {
            log.warn("Listing already exists for process {}", processInstanceId);
            execution.setVariable("listingId", existing.get().getId());
            execution.removeVariable("createError");
            return;
        }

        try {
            String title = (String) execution.getVariable("title");
            String address = (String) execution.getVariable("address");
            String description = (String) execution.getVariable("description");
            Object priceObj = execution.getVariable("price");
            String ownerId = (String) execution.getVariable("initiatorUserId");

            if (isBlank(title)) throw new IllegalArgumentException("title is required");
            if (isBlank(address)) throw new IllegalArgumentException("address is required");
            if (priceObj == null) throw new IllegalArgumentException("price is required");
            if (isBlank(ownerId)) throw new IllegalArgumentException("initiatorUserId is required");

            BigDecimal price = parsePrice(priceObj);
            if (price.signum() < 0) throw new IllegalArgumentException("price must be non-negative");

            Listing listing = new Listing();
            listing.setTitle(title);
            listing.setAddress(address);
            listing.setDescription(description);
            listing.setPrice(price);
            listing.setStatus(ListingStatus.AVAILABLE);
            listing.setOwnerId(ownerId);
            listing.setProcessInstanceId(processInstanceId);
            listing.setCreatedAt(Instant.now());

            Listing saved = listingRepository.save(listing);
            execution.setVariable("listingId", saved.getId());
            execution.removeVariable("createError");

            log.info("Listing created: id={}, title='{}', owner={}", saved.getId(), title, ownerId);

        } catch (Exception e) {
            log.error("Failed to create listing", e);
            execution.setVariable("createError",
                    e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private BigDecimal parsePrice(Object priceObj) {
        if (priceObj instanceof BigDecimal bd) return bd;
        if (priceObj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(priceObj.toString());
    }
}