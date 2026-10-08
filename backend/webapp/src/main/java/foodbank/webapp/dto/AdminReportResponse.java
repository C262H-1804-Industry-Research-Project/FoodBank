package foodbank.webapp.dto;

import java.time.LocalDateTime;

public record AdminReportResponse(
        Integer reportId,
        Integer reporterId,
        Integer reportedId,
        Integer listingId,
        Integer pickupRequestId,
        String reportType,
        String reason,
        String details,
        String status,
        String adminNote,
        String outcome,
        Integer processedBy,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}