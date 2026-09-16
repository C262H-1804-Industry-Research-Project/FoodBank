package foodbank.webapp.dto;

public record CreatedListingResponse(
        Integer listingId,
        Integer donorId,
        String status
) {
}
