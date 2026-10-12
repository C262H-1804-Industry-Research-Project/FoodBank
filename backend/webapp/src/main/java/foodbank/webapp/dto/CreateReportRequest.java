package foodbank.webapp.dto;

import java.time.LocalDateTime;

public record CreateReportRequest(
        String reportType,
        Integer listingId,
        Integer pickupRequestId,
        Integer reportedUserId,
        String reason,
        String details,
        String evidenceImageUrl,
        LocalDateTime actualTransactionTime
) {
}
