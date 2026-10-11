package foodbank.webapp.dto;

public record AdminDonorAccountResponse(
        Integer donorId,
        Integer userId,
        String organisation,
        long reportsAgainst,
        long donationsMade,
        long successfulCollections,
        long banTimes,
        String status,
        Long banDaysRemaining
) {
}
