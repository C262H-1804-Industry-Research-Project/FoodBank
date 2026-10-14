package foodbank.webapp.dto;

import java.time.LocalDateTime;

// One row of a recipient's own report list. Display fields are null when the
// linked listing, donor or request is gone.
public record RecipientReportResponse(
        Integer reportId,
        Integer listingId,
        Integer pickupRequestId,
        String reason,
        String details,
        String status,
        String outcome,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        String listingName,
        String donorName,
        LocalDateTime timeslotStart,
        LocalDateTime timeslotEnd,
        String evidenceImageUrl
) {
}
