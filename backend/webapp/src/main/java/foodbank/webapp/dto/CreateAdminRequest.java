package foodbank.webapp.dto;

public record CreateAdminRequest(
        String name,
        String email,
        String password
) {
}
