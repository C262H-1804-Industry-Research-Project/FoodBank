package foodbank.webapp.dto;
public record AdminListingResponse(
        ListingResponse listing,
        String status,
        Float quantity,
        long completedRequests,
        long approvedRequests
) {}
