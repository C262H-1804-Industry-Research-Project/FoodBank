package foodbank.webapp.config;

import foodbank.webapp.entity.Listing;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.service.GeocodingService;
import foodbank.webapp.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, looks up coordinates for listings saved before coordinates existed
 * (or whose lookup failed), so they can be sorted and shown by distance.
 */
@Component
@RequiredArgsConstructor
public class ListingCoordinatesBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ListingCoordinatesBackfill.class);
    // Mapbox allows 10 requests per second by default
    private static final long DELAY_BETWEEN_LOOKUPS_MS = 150;

    private final ListingRepository listingRepository;
    private final ListingService listingService;
    private final GeocodingService geocodingService;

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        if (!geocodingService.isEnabled()) {
            log.info("mapbox.access-token is not set; listing distances are disabled.");
            return;
        }
        int updated = 0;
        for (Listing listing : listingRepository.findAll()) {
            if (listing.getLatitude() != null || listing.getAddressLine() == null || listing.getAddressLine().isBlank()) {
                continue;
            }
            listingService.updateCoordinates(listing);
            if (listing.getLatitude() != null) {
                listingRepository.save(listing);
                updated++;
            }
            Thread.sleep(DELAY_BETWEEN_LOOKUPS_MS);
        }
        if (updated > 0) log.info("Added pickup coordinates to {} listing(s).", updated);
    }
}
