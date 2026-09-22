package foodbank.webapp.dto;

public record MyListingResponse(
        Integer listingId,
        String name,
        String category,
        Float quantity,
        String status,
        String expiryDate,
        String collectionDeadline,
        String photoUrl,
        String[] tags,
        TimeSlot[] timeslots
) {}