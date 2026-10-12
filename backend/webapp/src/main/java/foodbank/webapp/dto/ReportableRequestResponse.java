package foodbank.webapp.dto;

// An approved pickup request a donor can file a report about,
// with the IDs that POST /api/reports needs.
public record ReportableRequestResponse(
        Integer requestId,
        Integer listingId,
        String listingName,
        Integer timeslotId,
        String timeslotStart,
        String timeslotEnd,
        Integer recipientUserId,
        String recipientName,
        Float quantityRequested
) {}
