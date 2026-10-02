package foodbank.webapp.dto;

public record ProcessReportRequest(
        String status,
        String adminNote,
        String outcome
) {
}