package foodbank.webapp.dto;

import java.time.LocalDateTime;

// The fields a reporter may change while their report is still under review.
public record UpdateReportRequest(
        String reason,
        String details,
        String evidenceImageUrl,
        LocalDateTime actualTransactionTime
) {
}
