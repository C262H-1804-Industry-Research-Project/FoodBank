package foodbank.webapp.dto;

public record DualRoleRegisterRequest(
        String email,
        String password,
        String phone,
        String donorOrgName,
        String addressLine,
        String suburb,
        String region,
        String recipientName,
        String recipientOrgName
) {
}