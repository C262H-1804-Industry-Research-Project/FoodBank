package foodbank.webapp.dto;

public record AdminDonorAccountResponse(
        Integer donorId,
        Integer userId,
        String organisation,
        String email,
        String phone,
        String suburb,
        String region,
        long reportsAgainst,
        long donationsMade,
        long successfulCollections,
        long banTimes,
        String status,
        Long banDaysRemaining
) {
}
