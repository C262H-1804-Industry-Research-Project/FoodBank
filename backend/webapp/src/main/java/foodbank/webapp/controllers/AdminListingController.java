package foodbank.webapp.controllers;

import foodbank.webapp.dto.BanListingResponse;
import foodbank.webapp.service.AdminListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/listings")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminListingController {

    private final AdminListingService adminListingService;
    private final foodbank.webapp.service.ListingService listingService;

    @org.springframework.web.bind.annotation.GetMapping
    public org.springframework.data.web.PagedModel<foodbank.webapp.dto.AdminListingResponse> getListings(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size");
        return new org.springframework.data.web.PagedModel<>(listingService.findAdminListings(
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("listingId"))));
    }

    @PostMapping("/{listingId}/ban")
    @ResponseStatus(HttpStatus.OK)
    public BanListingResponse banListing(
            @PathVariable Integer listingId
    ) {
        return adminListingService.banListing(listingId);
    }
}