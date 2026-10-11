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

    @PostMapping("/{listingId}/ban")
    @ResponseStatus(HttpStatus.OK)
    public BanListingResponse banListing(
            @PathVariable Integer listingId
    ) {
        return adminListingService.banListing(listingId);
    }
}