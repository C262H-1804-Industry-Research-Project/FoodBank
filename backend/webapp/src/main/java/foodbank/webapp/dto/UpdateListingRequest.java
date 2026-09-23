package foodbank.webapp.dto;

import java.time.LocalDate;

/** Editable listing fields. Photos, tags, expiry and pickup times stay unchanged. */
public record UpdateListingRequest(
        String name, String description, Integer categoryId, Float quantity,
        LocalDate collectionDeadline, String addressLine, String suburb, String region,
        Integer slotCapacity, Boolean isAutoApproved, Boolean usePickupCode, Boolean hideContact
) {}
