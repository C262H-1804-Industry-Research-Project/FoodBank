package foodbank.webapp.dto;

public record LoginUserResponse(
        String name,
        String role,
        Integer donorId
) {
}
