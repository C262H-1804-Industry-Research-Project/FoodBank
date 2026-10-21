package foodbank.webapp.controllers;

import foodbank.webapp.dto.CreateListingRequest;
import foodbank.webapp.dto.CreatedListingResponse;
import foodbank.webapp.dto.MyListingResponse;
import foodbank.webapp.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    private static final int MAX_PAGE_SIZE = 100;

    // One page of the donor's own listings, newest first
    @GetMapping
    public PagedModel<MyListingResponse> getMyListings(@PathVariable Integer donorId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "5") int size,
                                                       java.security.Principal principal) {
        listingService.requireDonorOwner(donorId, principal.getName());
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        return new PagedModel<>(listingService.getMyListings(donorId, PageRequest.of(page, size)));
    }
}
