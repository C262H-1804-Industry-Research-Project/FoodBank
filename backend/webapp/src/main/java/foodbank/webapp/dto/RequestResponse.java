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
        String note
) {}