package foodbank.webapp.dto;

public record AddRoleResponse(
        String message,
        String role,
        Integer profileId
) {
}
