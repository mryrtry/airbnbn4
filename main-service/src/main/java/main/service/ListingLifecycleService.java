package main.service;

import main.entity.Listing;
import main.entity.ListingStatus;
import main.exception.EntityNotFoundException;
import main.exception.ValidationException;
import main.repository.ListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class ListingLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ListingLifecycleService.class);

    private final ListingRepository listingRepository;

    public ListingLifecycleService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Transactional
    public Long create(String processInstanceId, String ownerId,
                       String title, String address, String description, BigDecimal price) {

        Optional<Listing> existing = listingRepository.findByProcessInstanceId(processInstanceId);
        if (existing.isPresent()) {
            log.warn("Listing already exists for process {}", processInstanceId);
            return existing.get().getId();
        }

        validate(title, address, price, ownerId);

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
        log.info("Listing created: id={}, title='{}', owner={}", saved.getId(), title, ownerId);
        return saved.getId();
    }

    @Transactional
    public void update(Long listingId, String title, String address,
                       String description, BigDecimal price) {
        validate(title, address, price, null);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + listingId));

        listing.setTitle(title);
        listing.setAddress(address);
        listing.setDescription(description);
        listing.setPrice(price);
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        log.info("Listing {} updated", listingId);
    }

    @Transactional
    public void softDelete(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + listingId));

        Instant now = Instant.now();
        listing.setStatus(ListingStatus.DELETED);
        listing.setDeletedAt(now);
        listing.setUpdatedAt(now);
        listingRepository.save(listing);

        log.info("Listing {} soft-deleted", listingId);
    }

    private void validate(String title, String address, BigDecimal price, String ownerId) {
        if (isBlank(title)) {
            throw new ValidationException("title is required");
        }
        if (isBlank(address)) {
            throw new ValidationException("address is required");
        }
        if (price == null) {
            throw new ValidationException("price is required");
        }
        if (price.signum() < 0) {
            throw new ValidationException("price must be non-negative");
        }
        if (ownerId != null && isBlank(ownerId)) {
            throw new ValidationException("initiatorUserId is required");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}