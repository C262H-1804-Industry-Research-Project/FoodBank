package foodbank.webapp.dto;

public record DonorResponse(
        Integer donorId,
        Integer userId,
        String email,
        String orgName,
        String addressLine,
        String suburb,
        String region,
        String approvalStatus,
        Boolean hideContact
) {
}
