package foodbank.webapp.dto;

public record RecipientLocation(
        String addressLine,
        String suburb,
        String region,
        Double latitude,
        Double longitude
) {}
