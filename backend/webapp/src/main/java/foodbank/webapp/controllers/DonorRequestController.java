package foodbank.webapp.controllers;

import foodbank.webapp.dto.ReportableRequestResponse;
import foodbank.webapp.dto.RequestResponse;
import foodbank.webapp.service.RequestService;
import foodbank.webapp.service.ListingService;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/donors/{donorId}/requests")
@RequiredArgsConstructor
public class DonorRequestController {
    private final RequestService requestService;
    private final ListingService listingService;

    @GetMapping
    public List<RequestResponse> getRequests(@PathVariable Integer donorId, Authentication authentication) {
        listingService.requireDonorOwner(donorId, authentication.getName());
        return requestService.getDonorRequests(donorId);
    }

    @GetMapping("/reportable")
    public List<ReportableRequestResponse> getReportableRequests(@PathVariable Integer donorId) {
        return requestService.getReportableRequests(donorId);
    }

    @PatchMapping("/{requestId}/approve")
    public RequestResponse approveRequest(@PathVariable Integer donorId, @PathVariable Integer requestId,
                                          Authentication authentication) {
        listingService.requireDonorOwner(donorId, authentication.getName());
        return requestService.approveRequest(donorId, requestId);
    }

    @PatchMapping("/{requestId}/reject")
    public RequestResponse rejectRequest(@PathVariable Integer donorId, @PathVariable Integer requestId,
                                         Authentication authentication) {
        listingService.requireDonorOwner(donorId, authentication.getName());
        return requestService.rejectRequest(donorId, requestId);
    }
}