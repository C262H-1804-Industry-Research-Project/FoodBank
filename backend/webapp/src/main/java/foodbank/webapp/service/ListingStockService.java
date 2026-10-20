package foodbank.webapp.service;

import foodbank.webapp.entity.Listing;
import foodbank.webapp.exceptions.ConflictException;
import foodbank.webapp.repositories.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Available stock is worked out from requests rather than stored, so it cannot drift.
 * A request holds its quantity from the moment it is made until it is rejected or cancelled,
 * or, once EXPIRED, until StockReleaseService releases it.
 */
@Service
@RequiredArgsConstructor
public class ListingStockService {
    // EXPIRED holds stock only until it has a stockReleasedAt; see sumQuantityForListing
    public static final List<String> HOLDING_STATUSES = List.of("PENDING", "APPROVED", "COMPLETED", "EXPIRED");

    private final RequestRepository requestRepository;

    public float held(Listing listing) {
        Double sum = requestRepository.sumQuantityForListing(listing.getListingId(), HOLDING_STATUSES);
        return sum == null ? 0f : sum.floatValue();
    }

    public float available(Listing listing) {
        float total = listing.getQuantity() == null ? 0f : listing.getQuantity();
        return Math.max(0f, total - held(listing));
    }

    // Callers lock the listing row first so two requests cannot take the same stock
    public void requireAvailable(Listing listing, float quantity) {
        float available = available(listing);
        if (available <= 0) {
            throw new ConflictException("This listing is fully reserved");
        }
        if (quantity > available) {
            throw new ConflictException("Only " + format(available) + " left for this listing");
        }
    }

    public static String format(float quantity) {
        return new BigDecimal(Float.toString(quantity)).stripTrailingZeros().toPlainString();
    }
}
