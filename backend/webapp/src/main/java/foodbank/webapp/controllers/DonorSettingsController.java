package foodbank.webapp.controllers;

import foodbank.webapp.dto.*;
import foodbank.webapp.service.DonorSettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DonorSettingsController {
    private final DonorSettingsService donorSettingsService;

    @GetMapping("/api/donors/{donorId}/settings")
    public DonorSettingsResponse getSettings(@PathVariable Integer donorId) {
        return donorSettingsService.getSettings(donorId);
    }

    @PutMapping("/api/donors/{donorId}/settings")
    public DonorSettingsResponse updateSettings(
            @PathVariable Integer donorId,
            @RequestBody UpdateDonorSettingsRequest request
    ) {
        return donorSettingsService.updateSettings(donorId, request);
    }

    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@RequestBody CreateReportRequest request) {
        return donorSettingsService.createReport(request);
    }
}
