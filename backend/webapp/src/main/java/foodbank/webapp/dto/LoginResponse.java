package foodbank.webapp.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        LoginUserResponse user
) {
}