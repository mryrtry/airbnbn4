package main.service;

import main.dto.ListingDto;
import main.entity.Listing;
import main.entity.ListingStatus;
import main.exception.EntityNotFoundException;
import main.repository.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListingService {

    private final ListingRepository repository;

    public ListingService(ListingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ListingDto> getAvailableListings() {
        return repository.findByStatus(ListingStatus.AVAILABLE)
                .stream()
                .map(ListingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingDto> getMyListings(String ownerId) {
        return repository.findByOwnerIdAndStatusNot(ownerId, ListingStatus.DELETED)
                .stream()
                .map(ListingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ListingDto getListing(Long id) {
        Listing listing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + id));

        if (listing.getStatus() == ListingStatus.DELETED) {
            throw new EntityNotFoundException("Listing not found: " + id);
        }

        return ListingDto.from(listing);
    }
}