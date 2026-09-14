package main.repository;

import main.entity.Listing;
import main.entity.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByOwnerIdAndStatusNot(String ownerId, ListingStatus status);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByStatusNot(ListingStatus status);

    Optional<Listing> findByProcessInstanceId(String processInstanceId);
}