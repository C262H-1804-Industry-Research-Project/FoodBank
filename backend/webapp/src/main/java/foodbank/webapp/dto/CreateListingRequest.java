package foodbank.webapp.dto;

import java.time.LocalDate;
import java.util.List;

public record CreateListingRequest(
        String name,
        String description,
        String category,
        Float quantity,
        LocalDate expiryDate,
        LocalDate collectionDeadline,
        String pickupAddress,
        Integer maxDailyCapacity,
        String photoUrl,
        String notes,
        List<String> tags,
        List<CreateTimeslotRequest> timeslots
) {
}
