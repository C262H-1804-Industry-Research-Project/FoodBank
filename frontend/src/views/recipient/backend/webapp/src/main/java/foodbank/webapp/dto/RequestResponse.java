package foodbank.webapp.dto;

public record RequestResponse(
        Integer requestId,
        String recipientName,
        String listingName,
        String timeslotStart,
        String timeslotEnd,
        Float quantityRequested,
        String status,
        String pickupCode,
        String note,
        String donorName,
        String pickupAddress,
        String recipientOrg,
        String recipientPhone,
        Float listingQuantity
) {}