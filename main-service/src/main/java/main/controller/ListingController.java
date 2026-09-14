package main.controller;

import main.dto.ListingDto;
import main.dto.ProcessStartResult;
import main.service.ListingProcessService;
import main.service.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;
    private final ListingProcessService processService;

    public ListingController(ListingService listingService,
                             ListingProcessService processService) {
        this.listingService = listingService;
        this.processService = processService;
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission('LISTING_READ')")
    public ResponseEntity<List<ListingDto>> getAvailable() {
        return ResponseEntity.ok(listingService.getAvailableListings());
    }

    @GetMapping("/my")
    @PreAuthorize("@permissionService.hasPermission('LISTING_READ')")
    public ResponseEntity<List<ListingDto>> getMy(Authentication auth) {
        return ResponseEntity.ok(listingService.getMyListings(auth.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('LISTING_READ')")
    public ResponseEntity<ListingDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getListing(id));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('LISTING_CREATE')")
    public ResponseEntity<ProcessStartResult> create(Authentication auth) {
        return ResponseEntity.ok(processService.startCreate(auth.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('LISTING_UPDATE')")
    public ResponseEntity<ProcessStartResult> update(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(processService.startUpdate(auth.getName(), id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('LISTING_DELETE')")
    public ResponseEntity<ProcessStartResult> delete(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(processService.startDelete(auth.getName(), id));
    }
}