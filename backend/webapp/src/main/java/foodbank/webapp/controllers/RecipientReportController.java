package foodbank.webapp.controllers;

import foodbank.webapp.dto.RecipientReportResponse;
import foodbank.webapp.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/recipients/me/reports")
@RequiredArgsConstructor
public class RecipientReportController {

    private final ReportService reportService;

    private static final int MAX_PAGE_SIZE = 100;

    // The signed-in recipient's own reports, newest first. status is a report status code
    // and q matches the listing, donor, reason or details; leave either out to not filter by it.
    @GetMapping
    public PagedModel<RecipientReportResponse> getMyReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE
            );
        }
        // JwtAuthenticationFilter stores the user ID as the principal name.
        return new PagedModel<>(reportService.getReportsByRecipient(
                Integer.valueOf(authentication.getName()), status, q, PageRequest.of(page, size)
        ));
    }
}
