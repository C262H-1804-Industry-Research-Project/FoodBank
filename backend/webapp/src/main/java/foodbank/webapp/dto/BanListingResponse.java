package foodbank.webapp.dto;

public record BanListingResponse(
        String message,
        Integer listingId,
        String status
) {
}