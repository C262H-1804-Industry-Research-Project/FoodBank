package foodbank.webapp.dto;

public record CreateReportRequest(
        Integer reporterId,
        Integer reportedId,
        Integer listingId,
        Integer requestId,
        String reason
) {}
