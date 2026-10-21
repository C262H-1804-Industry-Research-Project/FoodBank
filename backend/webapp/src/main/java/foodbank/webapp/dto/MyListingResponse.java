package foodbank.webapp.dto;

public record MyListingResponse(
        Integer listingId,
        String name,
        String category,
        Float quantity,
        String status,
        String expiryDate,
        String collectionDeadline,
        Integer maxDailyCapacity,
        String photoUrl,
        String[] tags,
        TimeSlot[] timeslots,
        String suburb,
        String region,
        Integer categoryId,
        String description,
        String addressLine,
        Boolean isAutoApproved,
        Boolean usePickupCode,
        Boolean hideContact,
        Integer slotCapacity,
        long completedRequests,
        long approvedRequests
) {}
