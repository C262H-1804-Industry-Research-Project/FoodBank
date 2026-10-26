package foodbank.webapp.dto;

public record AddRoleRequest(
        String targetRole,
        String donorOrgName,
        String addressLine,
        String suburb,
        String region,
        String recipientName,
        String recipientOrgName
) {
}
