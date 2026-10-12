package foodbank.webapp.controllers;

import foodbank.webapp.dto.DonorReportResponse;
import foodbank.webapp.dto.ReportResponse;
import foodbank.webapp.dto.UpdateReportRequest;
import foodbank.webapp.service.ListingService;
import foodbank.webapp.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/donors/{donorId}/reports")
@RequiredArgsConstructor
public class DonorReportController {

    private final ListingService listingService;
    private final ReportService reportService;

    @GetMapping
    public List<DonorReportResponse> getDonorReports(
            @PathVariable Integer donorId,
            Principal principal
    ) {
        listingService.requireDonorOwner(donorId, principal.getName());
        return reportService.getReportsByDonor(donorId);
    }

    @PutMapping("/{reportId}")
    public ReportResponse updateDonorReport(
            @PathVariable Integer donorId,
            @PathVariable Integer reportId,
            @RequestBody UpdateReportRequest request,
            Principal principal
    ) {
        listingService.requireDonorOwner(donorId, principal.getName());
        return reportService.updateDonorReport(donorId, reportId, request);
    }

    @PatchMapping("/{reportId}/cancel")
    public ReportResponse cancelDonorReport(
            @PathVariable Integer donorId,
            @PathVariable Integer reportId,
            Principal principal
    ) {
        listingService.requireDonorOwner(donorId, principal.getName());
        return reportService.cancelDonorReport(donorId, reportId);
    }
}
