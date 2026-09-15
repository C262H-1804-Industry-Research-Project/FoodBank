package foodbank.webapp.dto;

import foodbank.webapp.entity.Listing;

import java.util.List;

public record ListingListResponse(
        int length,
        List<ListingResponse> data
) {}
