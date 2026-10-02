package foodbank.webapp.controllers;

import foodbank.webapp.dto.RequestSubmission;
import foodbank.webapp.dto.RequestResponse;
import foodbank.webapp.service.RequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public RequestResponse createRequest(@RequestBody RequestSubmission request) {
        return requestService.createRequest(request);
    }

    @GetMapping
    public List<RequestResponse> getMyRequests(@RequestParam Integer recipientId) {
        return requestService.getMyRequests(recipientId);
    }
}
