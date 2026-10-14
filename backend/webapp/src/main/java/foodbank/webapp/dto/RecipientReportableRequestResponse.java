package foodbank.webapp.dto;

// An approved pickup request a recipient can file a report about,
// with the IDs that POST /api/reports needs.
public record RecipientReportableRequestResponse(
        Integer requestId,
        Integer listingId,
        String listingName,
        String donorName,
        String timeslotStart,
        String timeslotEnd,
        Float quantityRequested
) {}
