package foodbank.webapp.dto;

import foodbank.webapp.entity.Listing;

import java.util.Date;

public record ListingResponse(
        String foodName,
        String description,
        String category,
        String expiryDate,
        String latestCollectionDate,
        String[] tags,
        TimeSlot[] timeslots
){
    public static ListingResponse fromEntity(Listing listing) {
        return new ListingResponse(
                listing.getName(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getExpiryDate() == null ? null : listing.getExpiryDate().toString(),
                listing.getCollectionDeadline() == null ? null : listing.getCollectionDeadline().toString(),
                new String[0],
                new TimeSlot[0]
        );
    }
}
