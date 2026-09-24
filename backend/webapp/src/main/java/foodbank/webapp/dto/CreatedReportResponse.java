package foodbank.webapp.dto;

import java.time.LocalDateTime;

public record CreatedReportResponse(
        String message,
        Integer reportId,
        String status,
        LocalDateTime createdAt
) {
}