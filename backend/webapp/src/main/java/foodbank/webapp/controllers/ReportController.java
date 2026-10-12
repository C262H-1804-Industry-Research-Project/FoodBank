package foodbank.webapp.controllers;

import foodbank.webapp.dto.CreateReportRequest;
import foodbank.webapp.dto.CreatedReportResponse;
import foodbank.webapp.entity.Report;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/statuses")
    public List<String> getReportStatuses() {
        return Report.STATUSES;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedReportResponse submitReport(
            @RequestBody CreateReportRequest request,
            Authentication authentication
    ) {
        /*
         * JwtAuthenticationFilter stores the authenticated
         * user ID as the Authentication principal.
         */
        Integer reporterUserId =
                Integer.valueOf(authentication.getName());

        String reporterRole = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority ->
                        authority.startsWith("ROLE_")
                )
                .findFirst()
                .map(authority ->
                        authority.substring(5)
                )
                .orElseThrow(() ->
                        new ForbiddenException(
                                "Donor or Recipient role required"
                        )
                );

        return reportService.submitReport(
                request,
                reporterUserId,
                reporterRole
        );
    }
}