package foodbank.webapp.dto;

public record DonorSettingsResponse(
        Integer donorId,
        Boolean hideContact,
        String addressLine,
        String suburb,
        String region
) {}