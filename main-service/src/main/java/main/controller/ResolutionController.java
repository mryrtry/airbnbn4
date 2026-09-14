package main.controller;

import jakarta.validation.Valid;
import main.dto.FileComplaintRequest;
import main.dto.ProcessStartResult;
import main.service.ResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resolutions")
public class ResolutionController {

    private final ResolutionService resolutionService;

    public ResolutionController(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('RESOLUTION_OPEN')")
    public ResponseEntity<ProcessStartResult> fileComplaint(
            @Valid @RequestBody FileComplaintRequest req,
            Authentication auth) {

        resolutionService.fileComplaint(
                req.bookingId(), auth.getName(), req.reason(), req.ownerComment());

        return ResponseEntity.ok(new ProcessStartResult(
                null, "FILE_COMPLAINT", req.bookingId(), "filed"));
    }
}