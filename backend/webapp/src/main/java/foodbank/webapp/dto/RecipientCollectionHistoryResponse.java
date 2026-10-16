package foodbank.webapp.dto;

/**
 * One completed collection displayed in the
 * recipient collection-history page.
 */
public record RecipientCollectionHistoryResponse(
        Integer historyId,
        Integer requestId,
        Integer listingId,
        String foodName,
        String category,
        Float quantityCollected,
        String donorName,
        String collectedDatetime,
        String pickupLocation
) {
}