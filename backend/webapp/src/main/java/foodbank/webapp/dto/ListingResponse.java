package foodbank.webapp.dto;

import foodbank.webapp.entity.Listing;

import java.util.Date;

public record ListingResponse(
        Integer listingId,
        String foodName,
        String description,
        String category,
        String expiryDate,
        Integer maxDailyCapacity,
        String latestCollectionDate,
        String[] tags,
        TimeSlot[] timeslots,
        String donorName,
        String suburb,
        String region
){ }
