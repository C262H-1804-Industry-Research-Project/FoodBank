package foodbank.webapp.controllers;

import foodbank.webapp.dto.DonorSettingsResponse;
import foodbank.webapp.dto.UpdateDonorSettingsRequest;
import foodbank.webapp.service.DonorSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DonorSettingsController {

    private final DonorSettingsService donorSettingsService;

    @GetMapping("/api/donors/{donorId}/settings")
    public DonorSettingsResponse getSettings(
            @PathVariable Integer donorId
    ) {
        return donorSettingsService.getSettings(donorId);
    }

    @PutMapping("/api/donors/{donorId}/settings")
    public DonorSettingsResponse updateSettings(
            @PathVariable Integer donorId,
            @RequestBody UpdateDonorSettingsRequest request
    ) {
        return donorSettingsService.updateSettings(
                donorId,
                request
        );
    }
}
