package foodbank.webapp.dto;

public record RequestSubmission(
        Integer timeslotId,
        Float quantityRequested,
        String note
) {}