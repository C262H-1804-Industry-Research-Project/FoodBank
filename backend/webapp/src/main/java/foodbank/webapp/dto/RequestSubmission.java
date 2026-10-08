package foodbank.webapp.dto;

public record RequestSubmission(
        Integer timeslotId,
        Integer recipientId,
        Float quantityRequested,
        String note
) {}