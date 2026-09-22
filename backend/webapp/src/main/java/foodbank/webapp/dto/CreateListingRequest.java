package foodbank.webapp.dto;

import java.time.LocalDate;
import java.util.List;

public record CreateListingRequest(
        String name,
        String description,
        Integer categoryId,
        Float quantity,
        LocalDate expiryDate,
        LocalDate collectionDeadline,
        String addressLine,
        String suburb,
        String region,
        Integer maxDailyCapacity,
        String photoUrl,
        List<Integer> tagIds,
        List<CreateTimeslotRequest> timeslots,
        Boolean isAutoApproved,
        Boolean usePickupCode,
        String status
) {
}
