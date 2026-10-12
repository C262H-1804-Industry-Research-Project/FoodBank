package foodbank.webapp.dto;

public record AdminRecipientAccountResponse(
        Integer recipientId,
        Integer userId,
        String recipient,
        String email,
        String phone,
        String suburb,
        String region,
        long reportsAgainst,
        long requestsMade,
        long successfulCollections,
        long banTimes,
        String status,
        Long banDaysRemaining
) {
}
