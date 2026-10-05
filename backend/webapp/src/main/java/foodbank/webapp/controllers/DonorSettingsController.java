package foodbank.webapp.controllers;

import foodbank.webapp.dto.DonorSettingsResponse;
import foodbank.webapp.dto.UpdateDonorSettingsRequest;
import foodbank.webapp.service.DonorSettingsService;
import foodbank.webapp.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DonorSettingsController {

    private final DonorSettingsService donorSettingsService;
    private final ListingService listingService;

    @GetMapping("/api/donors/{donorId}/settings")
    public DonorSettingsResponse getSettings(
            @PathVariable Integer donorId,
            Principal principal
    ) {
        listingService.requireDonorOwner(donorId, principal.getName());
        return donorSettingsService.getSettings(donorId);
    }

    @PutMapping("/api/donors/{donorId}/settings")
    public DonorSettingsResponse updateSettings(
            @PathVariable Integer donorId,
            @RequestBody UpdateDonorSettingsRequest request,
            Principal principal
    ) {
        listingService.requireDonorOwner(donorId, principal.getName());
        return donorSettingsService.updateSettings(
                donorId,
                request
        );
    }
}
