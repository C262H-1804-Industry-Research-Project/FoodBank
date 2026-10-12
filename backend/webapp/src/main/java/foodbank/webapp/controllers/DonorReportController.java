package foodbank.webapp.controllers;

import foodbank.webapp.dto.DonorReportResponse;
import foodbank.webapp.service.ListingService;
import foodbank.webapp.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/donors/{donorId}/reports")
@RequiredArgsConstructor
public class DonorReportController {

    private final ListingService listingService;
    private final ReportService reportService;

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public PagedModel<DonorReportResponse> getDonorReports(
            @PathVariable Integer donorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        listingService.requireDonorOwner(donorId, principal.getName());
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE
            );
        }
        return new PagedModel<>(reportService.getReportsByDonor(
                donorId, status, q, PageRequest.of(page, size)
        ));
    }
}
