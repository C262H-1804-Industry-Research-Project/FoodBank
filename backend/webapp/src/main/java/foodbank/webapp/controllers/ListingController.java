package foodbank.webapp.controllers;

import foodbank.webapp.dto.ListingResponse;
import foodbank.webapp.dto.CatalogueItemResponse;
import foodbank.webapp.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public PagedModel<ListingResponse> getListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return new PagedModel<>(listingService.findAllListingResponse(pageable));
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
