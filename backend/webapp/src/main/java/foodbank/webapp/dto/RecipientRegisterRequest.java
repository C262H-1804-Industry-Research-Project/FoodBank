package foodbank.webapp.dto;

public record RecipientRegisterRequest(
        String name,
        String phone,
        String orgName,
        String email,
        String password
) {
}