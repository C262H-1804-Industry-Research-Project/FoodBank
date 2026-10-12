package foodbank.webapp.dto;

// One row of the admin account list. role is DONOR or RECIPIENT; activityCount is
// donations made for a donor and requests made for a recipient.
public record AdminAccountResponse(
        Integer userId,
        String role,
        String name,
        String email,
        String phone,
        String suburb,
        String region,
        String status,
        long reportsAgainst,
        long activityCount,
        long successfulCollections,
        long banTimes,
        Long banDaysRemaining
) {
}
