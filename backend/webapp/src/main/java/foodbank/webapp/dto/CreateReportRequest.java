package foodbank.webapp.dto;

public record CreateReportRequest(
        String reportType,
        Integer listingId,
        Integer pickupRequestId,
        Integer reportedUserId,
        String reason,
        String details
) {
}
