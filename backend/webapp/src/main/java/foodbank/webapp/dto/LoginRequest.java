package foodbank.webapp.dto;

public record LoginRequest(
        String email,
        String password,
        String role
) {
}
