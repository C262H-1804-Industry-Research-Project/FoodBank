package foodbank.webapp.controllers;

import foodbank.webapp.dto.CreateListingRequest;
import foodbank.webapp.dto.CreatedListingResponse;
import foodbank.webapp.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
    public CreatedListingResponse updateListing(
            @PathVariable Integer donorId,
            @PathVariable Integer listingId,
            @RequestBody CreateListingRequest request
    ) {
        return listingService.updateListing(donorId, listingId, request);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteListing(
            @PathVariable Integer donorId,
            @PathVariable Integer listingId
    ) {
        listingService.deleteListing(donorId, listingId);
    }
}
