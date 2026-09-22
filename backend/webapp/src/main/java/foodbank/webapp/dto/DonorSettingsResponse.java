package foodbank.webapp.dto;

public record DonorSettingsResponse(
        Integer donorId,
        Boolean hideContact
) {}