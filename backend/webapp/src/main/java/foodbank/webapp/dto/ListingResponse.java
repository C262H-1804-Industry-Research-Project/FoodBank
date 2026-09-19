package foodbank.webapp.dto;

import foodbank.webapp.entity.Listing;

import java.util.Date;

public record ListingResponse(
        Integer id,
        String foodName,
        String description,
        String category,
        String expiryDate,
        String latestCollectionDate,
        String[] tags,
        TimeSlot[] timeslots,
        Integer listingId,
        String donorName,
        String suburb,
        String region
){ }
