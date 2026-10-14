package foodbank.webapp.controllers;

import foodbank.webapp.dto.RecipientReportableRequestResponse;
import foodbank.webapp.dto.RequestSubmission;
import foodbank.webapp.dto.RequestResponse;
import foodbank.webapp.service.RequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RecipientRequestController {
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse createRequest(@RequestBody RequestSubmission request,
                                         Authentication authentication) {
        // JwtAuthenticationFilter stores the user ID as the principal name.
        return requestService.createRequest(request, Integer.valueOf(authentication.getName()));
    }

    @GetMapping
    public List<RequestResponse> getMyRequests(Authentication authentication) {
        return requestService.getMyRequests(Integer.valueOf(authentication.getName()));
    }

    // The signed-in recipient's approved requests, which they can file a report about.
    @GetMapping("/reportable")
    public List<RecipientReportableRequestResponse> getReportableRequests(Authentication authentication) {
        return requestService.getRecipientReportableRequests(Integer.valueOf(authentication.getName()));
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRequest(@PathVariable Integer requestId, Authentication authentication) {
        requestService.cancelRequest(requestId, Integer.valueOf(authentication.getName()));
    }
}
