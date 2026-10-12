package foodbank.webapp.controllers;

import foodbank.webapp.dto.ReportableRequestResponse;
import foodbank.webapp.dto.RequestResponse;
import foodbank.webapp.service.RequestService;
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

    @GetMapping
    public List<RequestResponse> getPendingRequests(@PathVariable Integer donorId) {
        return requestService.getPendingRequests(donorId);
    }

    @GetMapping("/reportable")
    public List<ReportableRequestResponse> getReportableRequests(@PathVariable Integer donorId) {
        return requestService.getReportableRequests(donorId);
    }

    @PatchMapping("/{requestId}/approve")
    public RequestResponse approveRequest(@PathVariable Integer donorId, @PathVariable Integer requestId) {
        return requestService.approveRequest(requestId);
    }

    @PatchMapping("/{requestId}/reject")
    public RequestResponse rejectRequest(@PathVariable Integer donorId, @PathVariable Integer requestId) {
        return requestService.rejectRequest(requestId);
    }
}