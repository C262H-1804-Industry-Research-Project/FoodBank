package foodbank.webapp.controllers;

import foodbank.webapp.dto.CreateListingRequest;
import foodbank.webapp.dto.CreatedListingResponse;
import foodbank.webapp.dto.MyListingResponse;
import foodbank.webapp.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/donors/{donorId}/listings")
@RequiredArgsConstructor
public class DonorListingController {
    private final ListingService listingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedListingResponse createListing(
            @PathVariable Integer donorId,
            @RequestBody CreateListingRequest request
    ) {
        return listingService.createListing(donorId, request);
    }
    @GetMapping
    public List<MyListingResponse> getMyListings(@PathVariable Integer donorId) {
        return listingService.getMyListings(donorId);
    }
}
