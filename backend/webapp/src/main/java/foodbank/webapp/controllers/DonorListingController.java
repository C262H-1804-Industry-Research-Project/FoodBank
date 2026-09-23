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
    @PutMapping("/{listingId}")
    public MyListingResponse updateListing(@PathVariable Integer donorId, @PathVariable Integer listingId,
            @RequestBody foodbank.webapp.dto.UpdateListingRequest request, java.security.Principal principal) {
        listingService.requireDonorOwner(donorId, principal.getName());
        return listingService.updateListing(donorId, listingId, request);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteListing(@PathVariable Integer donorId, @PathVariable Integer listingId, java.security.Principal principal) {
        listingService.requireDonorOwner(donorId, principal.getName());
        listingService.deleteListing(donorId, listingId);
    }

    @GetMapping
    public List<MyListingResponse> getMyListings(@PathVariable Integer donorId) {
        return listingService.getMyListings(donorId);
    }
}
