package foodbank.webapp.service;

import foodbank.webapp.dto.BanListingResponse;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.exceptions.ConflictException;
import foodbank.webapp.repositories.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdminListingService {

    private final ListingRepository listingRepository;

    @Transactional
    public BanListingResponse banListing(Integer listingId) {

        if (listingId == null) {
            throw new IllegalArgumentException(
                    "listingId is required"
            );
        }

        Listing listing = listingRepository
                .findById(listingId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Listing does not exist"
                        )
                );

        if ("BANNED".equalsIgnoreCase(listing.getStatus())) {
            throw new ConflictException(
                    "Listing is already banned"
            );
        }

        listing.setStatus("BANNED");
        listingRepository.save(listing);

        return new BanListingResponse(
                "Listing banned successfully",
                listing.getListingId(),
                listing.getStatus()
        );
    }
}