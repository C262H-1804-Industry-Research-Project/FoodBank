package foodbank.webapp.dto;

public record CreateDonorRequest(
        String email,
        String password,
        String phone,
        String orgName,
        String addressLine,
        String suburb,
        String region
) {
}
