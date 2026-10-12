package foodbank.webapp.dto;

public record UnbanUserResponse(
        String message,
        Integer userId,
        String userStatus
) {
}
