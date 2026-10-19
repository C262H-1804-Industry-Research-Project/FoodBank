package foodbank.webapp.controllers;

import foodbank.webapp.dto.CollectionHistoryResponse;
import foodbank.webapp.service.CollectionHistoryService;
import foodbank.webapp.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/donors/{donorId}/collection-history")
@RequiredArgsConstructor
public class DonorHistoryController {
    private final CollectionHistoryService collectionHistoryService;
    private final ListingService listingService;

    @GetMapping
    public List<CollectionHistoryResponse> getHistory(@PathVariable Integer donorId, Principal principal) {
        listingService.requireDonorOwner(donorId, principal.getName());
        return collectionHistoryService.getHistoryByDonor(donorId);
    }
}
