package foodbank.webapp.dto;

import java.time.LocalDateTime;

public record DonorReportResponse(
        Integer reportId,
        Integer reportedId,
        Integer listingId,
        Integer pickupRequestId,
        String reportType,
        String reason,
        String details,
        String status,
        String outcome,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // Display fields for the donor's report list; null when the linked record is gone.
        String listingName,
        String reportedName,
        LocalDateTime timeslotStart,
        LocalDateTime timeslotEnd,
        String evidenceImageUrl,
        LocalDateTime actualTransactionTime
) {
}
