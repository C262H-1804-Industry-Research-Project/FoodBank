package foodbank.webapp.controllers;

import foodbank.webapp.dto.ListingResponse;
import foodbank.webapp.dto.CatalogueItemResponse;
import foodbank.webapp.service.GeocodingService;
import foodbank.webapp.service.ListingService;
import foodbank.webapp.service.RecipientLocationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;
    private final RecipientLocationService recipientLocationService;

    @GetMapping
    public PagedModel<ListingResponse> getListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String foodName,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) List<Integer> tagIds,
            @RequestParam(required = false) String suburb,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size);
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be given together");
        }
        GeocodingService.Coordinates origin = null;
        if (latitude != null) {
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("latitude or longitude is out of range");
            }
            origin = new GeocodingService.Coordinates(latitude, longitude);
        } else if (isRecipient(authentication)) {
            // No location given: use the one the signed-in recipient saved, if any
            origin = recipientLocationService
                    .findSavedCoordinates(Integer.valueOf(authentication.getName()))
                    .orElse(null);
        }

        return new PagedModel<>(listingService.searchListingResponses(foodName, categoryId, tagIds, suburb, region, origin, pageable));
    }

    private static boolean isRecipient(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RECIPIENT".equals(authority.getAuthority()));
    }

    @GetMapping("/categories")
    public List<CatalogueItemResponse> getAvailableCategories() {
        return listingService.findAvailableCategories();
    }

    @GetMapping("/tags")
    public List<CatalogueItemResponse> getAvailableTags() {
        return listingService.findAvailableTags();
    }

}
